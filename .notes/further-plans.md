# Further Plans

## Anonymous cart before login

Идея: разрешить пользователю собирать корзину без регистрации и логина, а требовать JWT только
при checkout.

Это хороший UX для магазина, но не ближайший этап, потому что усложняет модель identity.

## Возможный flow

```text
1. Пользователь открывает каталог без JWT.
2. Пользователь добавляет товары в anonymous cart.
3. Клиент хранит cart id локально.
4. Перед checkout пользователь регистрируется или логинится.
5. Система связывает anonymous cart с authenticated user.
6. Order-service оформляет заказ только после JWT-проверки.
```

## Anonymous cart identity

До логина нет `X-User-Id`, поэтому корзину нельзя хранить только как:

```text
cart:user:{userId}
```

Нужен отдельный идентификатор:

```http
X-Cart-Id: <uuid>
```

Redis keys:

```text
cart:anon:{cartId}
cart:user:{userId}
```

`cartId` должен быть непрогнозируемым. Нельзя использовать простые числа, email или session-like
значения, которые легко угадать.

## Варианты создания cart id

### Вариант A. Клиент генерирует UUID

Клиент сам создает UUID и отправляет его в каждом cart request:

```http
X-Cart-Id: 550e8400-e29b-41d4-a716-446655440000
```

Плюсы:

- проще backend;
- не нужен endpoint для создания корзины.

Минусы:

- надо доверять клиенту в формате id;
- нужно валидировать UUID;
- потеря local storage означает потерю anonymous cart.

### Вариант B. Cart-service генерирует UUID

Первый `POST /cart/items` без `X-Cart-Id` создает cart id и возвращает его:

```http
X-Cart-Id: <generated-uuid>
```

Плюсы:

- backend контролирует формат;
- проще валидировать.

Минусы:

- нужно аккуратно документировать response header;
- frontend должен сохранить returned cart id.

## Merge после login

После login можно сделать один из вариантов.

### Вариант A. Explicit merge endpoint

```http
POST /api/cart/merge
Authorization: Bearer <accessToken>
X-Cart-Id: <anonymous-cart-id>
```

Gateway ставит:

```http
X-User-Id: <auth-user-id>
```

`cart-service` переносит товары:

```text
cart:anon:{cartId} -> cart:user:{userId}
```

Если один и тот же product есть в обеих корзинах, quantity суммируется с проверкой catalog.

### Вариант B. Merge during checkout

`order-service` получает и `X-User-Id`, и `X-Cart-Id`, затем оформляет заказ из anonymous cart.

Минус: checkout становится ответственным за слишком много поведения.

Для будущей реализации лучше `POST /api/cart/merge`.

## SecurityConfig для этого будущего варианта

```java
.pathMatchers("/api/auth/register", "/api/auth/login").permitAll()
.pathMatchers(HttpMethod.GET, "/api/catalog/products/**").permitAll()
.pathMatchers(HttpMethod.GET, "/api/catalog/categories/**").permitAll()
.pathMatchers("/api/cart/**").permitAll()
.anyExchange().authenticated()
```

Важно проверить: если authenticated пользователь отправляет JWT на public `/api/cart/**`,
gateway должен все равно распознать JWT и поставить `X-User-Id`. Если permitAll route не создает
principal, понадобится отдельная проверка/фильтр или явный merge endpoint.

## Почему откладываем

На ближайшем этапе достаточно:

```text
каталог public read
cart только с JWT
orders только с JWT
```

Anonymous cart стоит делать после того, как стабильно работают:

- auth через gateway;
- user profile;
- cart по `X-User-Id`;
- checkout из cart;
- role policy для catalog writes.

## Order-service future improvements

Сервис сейчас не проверяет, существует ли товар в catalog-service, не сверяет актуальную цену,
не списывает корзину и не меняет статус заказа после оплаты.

Enum уже содержит `PAID`, `CANCELLED`, `COMPLETED`, но публичных методов для смены статуса пока
нет. Сейчас основной сценарий: создать заказ и посмотреть свои заказы.
