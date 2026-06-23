# Gateway JWT And Order Headers Explained

## Что мы сделали

Раньше клиент мог сам отправить `userId` в `order-service`.

Это небезопасно:

```json
{
  "userId": 999,
  "items": []
}
```

Так клиент мог попытаться создать заказ от имени другого пользователя.

Теперь схема другая:

1. `auth-service` выдает JWT.
2. Клиент отправляет JWT в gateway:

```http
Authorization: Bearer <accessToken>
```

3. Gateway проверяет подпись JWT.
4. Gateway достает из JWT `userId`, `email`, `role`.
5. Gateway передает их дальше во внутренние сервисы через headers:

```http
X-User-Id: 4
X-User-Email: user@example.com
X-User-Role: USER
```

6. `order-service` доверяет `X-User-Id`, потому что этот header поставил gateway после проверки JWT.

## GatewayServiceApplication.java

```java
@SpringBootApplication
public class GatewayServiceApplication {
```

`@SpringBootApplication` говорит Spring Boot: это главный класс приложения.

Он включает:

- поиск Spring-компонентов внутри пакета `com.coolonlineshop.gateway`
- автоконфигурацию Spring Boot
- запуск embedded web server

```java
public static void main(String[] args) {
    SpringApplication.run(GatewayServiceApplication.class, args);
}
```

`main` запускает gateway-service.

## JwtConfig.java

```java
@Configuration
public class JwtConfig {
```

`@Configuration` означает: внутри класса есть Spring beans.

```java
@Bean
public SecretKey jwtSecretKey(@Value("${gateway.jwt.secret}") String jwtSecret) {
```

`@Bean` кладет объект в Spring context.

`@Value("${gateway.jwt.secret}")` берет секрет из `application.yml`.

Gateway должен использовать тот же секрет, что и `auth-service`, иначе он не сможет проверить подпись JWT.

```java
return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
```

Эта строка превращает обычную строку-секрет в ключ для HMAC SHA-256.

```java
@Bean
public ReactiveJwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
```

`ReactiveJwtDecoder` нужен WebFlux gateway, чтобы проверять JWT.

```java
return NimbusReactiveJwtDecoder.withSecretKey(jwtSecretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
```

Здесь мы говорим: JWT подписан алгоритмом `HS256`, проверяй его через наш shared secret.

## SecurityConfig.java

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
```

`@EnableWebFluxSecurity` включает security для reactive/WebFlux приложения.

Gateway использует WebFlux, потому что подключен:

```xml
spring-cloud-starter-gateway-server-webflux
```

```java
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
```

`SecurityWebFilterChain` - цепочка security-фильтров для gateway.

```java
.csrf(ServerHttpSecurity.CsrfSpec::disable)
```

CSRF выключен, потому что это API, а не HTML-форма с cookies.

```java
.pathMatchers("/api/auth/register", "/api/auth/login").permitAll()
```

Регистрация и логин публичные. Для них токен еще не нужен.

```java
.anyExchange().authenticated()
```

Все остальные routes требуют валидный JWT.

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

Эта строка включает режим resource server.

В нашем случае это значит: gateway принимает `Authorization: Bearer ...` и проверяет JWT через `ReactiveJwtDecoder`.

## JwtUserHeaderFilter.java

```java
@Component
public class JwtUserHeaderFilter implements GlobalFilter, Ordered {
```

`@Component` делает фильтр Spring-компонентом.

`GlobalFilter` означает: фильтр применяется ко всем gateway routes.

`Ordered` позволяет задать порядок выполнения фильтра.

```java
private static final String USER_ID_HEADER = "X-User-Id";
```

Это имя header, через который downstream services получают id пользователя.

```java
ServerWebExchange sanitizedExchange = removeUserHeaders(exchange);
```

Сначала gateway удаляет любые `X-User-*` headers, которые мог подделать клиент.

Это важно: клиенту нельзя доверять.

```java
return exchange.getPrincipal()
```

`getPrincipal()` возвращает текущего authenticated пользователя.

После успешной JWT-проверки это будет `JwtAuthenticationToken`.

```java
.filter(JwtAuthenticationToken.class::isInstance)
.cast(JwtAuthenticationToken.class)
```

Эти строки оставляют только JWT-authentication.

```java
.map(jwtAuthentication -> addUserHeaders(sanitizedExchange, jwtAuthentication))
```

Если JWT есть и он валидный, gateway добавляет доверенные headers.

```java
.defaultIfEmpty(sanitizedExchange)
```

Если пользователя нет, запрос идет дальше без user headers.

Но protected routes до этого не дойдут без JWT, потому что `SecurityConfig` вернет `401`.

```java
headers.set(USER_ID_HEADER, jwt.getSubject());
headers.set(USER_EMAIL_HEADER, jwt.getClaimAsString("email"));
headers.set(USER_ROLE_HEADER, jwt.getClaimAsString("role"));
```

Здесь данные берутся из уже проверенного JWT.

`subject` в нашем JWT - это id auth user.

## application.yml

```yaml
gateway:
  jwt:
    secret: ${AUTH_JWT_SECRET:dev-only-secret-key-for-auth-service-please-change}
```

Gateway берет тот же JWT secret, что и `auth-service`.

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
```

Для Spring Cloud Gateway `5.0.0` routes должны лежать под:

```text
spring.cloud.gateway.server.webflux.routes
```

Старый вариант `spring.cloud.gateway.routes` в этой версии не сработал.

```yaml
- RewritePath=/api/auth/?(?<segment>.*), /auth/$\{segment}
```

Gateway принимает внешний путь:

```text
/api/auth/register
```

А в `auth-service` отправляет:

```text
/auth/register
```

## Что изменилось в order-service

Создание заказа переведено на checkout:

```http
POST /orders/checkout
```

Тело запроса не нужно. Controller получает пользователя так:

```java
@RequestHeader("X-User-Id") Long userId
```

Это значит: `order-service` берет пользователя из header, который поставил gateway.

Состав заказа берется из `cart-service`, а `productName` и `productPrice` берутся из
`catalog-service`, а не из клиентского request body.

`GET /orders/{id}` теперь проверяет владельца заказа.

Если заказ принадлежит другому пользователю, сервис возвращает `404`.

Так мы не раскрываем, существует ли чужой заказ.
