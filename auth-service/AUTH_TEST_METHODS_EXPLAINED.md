# Auth Test Methods Explained

Этот файл разбирает выбранные тестовые методы из `AuthControllerTest` и `AuthIntegrationTest`.

## Главное Отличие Тестов

`AuthControllerTest` проверяет HTTP-слой отдельно.

В нём `AuthService` заменён mock-объектом. Это значит, что тест не ходит в базу, не проверяет настоящий hash пароля и не запускает Liquibase. Он проверяет, что controller:

- принимает правильный URL;
- принимает JSON body;
- возвращает правильный HTTP status;
- возвращает правильный JSON;
- правильно работает вместе с validation и `GlobalExceptionHandler`.

`AuthIntegrationTest` проверяет несколько слоёв вместе.

В нём используется настоящий Spring Boot context, настоящий `AuthService`, настоящий `AuthUserRepository`, настоящий `PasswordEncoder`, настоящий Liquibase и настоящий PostgreSQL в Docker-контейнере через Testcontainers.

Путь в integration test такой:

```text
HTTP request -> AuthController -> AuthService -> AuthUserRepository -> PostgreSQL -> HTTP response
```

## AuthControllerTest.registerReturnsCreatedAuthUser

```java
@Test
```

Говорит JUnit: метод ниже является тестом. Maven запускает такие методы при `mvn test`.

```java
void registerReturnsCreatedAuthUser() throws Exception {
```

Название метода описывает ожидаемое поведение: регистрация возвращает созданного auth-пользователя.

`void` значит, что метод ничего не возвращает.

`throws Exception` разрешает не писать `try/catch` вокруг `mockMvc.perform(...)`, потому что MockMvc-операции могут выбросить checked exception.

```java
AuthResponse response = new AuthResponse(
```

Создаём объект ответа, который mock-service позже вернёт controller'у.

В controller-test мы не проверяем настоящую бизнес-логику `AuthService`. Поэтому заранее готовим ответ вручную.

```java
        1L,
```

Первое поле `AuthResponse` - `userId`.

`1L` значит число `1` типа `Long`. В проекте `id` имеет тип `Long`, поэтому в тесте используем `1L`, а не просто `1`.

```java
        "ivan.user@example.com",
```

Второе поле `AuthResponse` - email пользователя.

```java
        Role.USER,
```

Третье поле - роль пользователя. Обычная регистрация создаёт пользователя с ролью `USER`.

```java
        "access-token"
```

Четвёртое поле - access token. В controller-test это просто строка-заглушка.

Мы не проверяем здесь настоящий JWT. Мы проверяем, что controller вернёт поле `accessToken` в JSON.

```java
);
```

Закрываем создание `AuthResponse`.

```java
when(authService.register(any(RegisterRequest.class))).thenReturn(response);
```

Настраиваем mock `AuthService`.

Смысл: если controller вызовет:

```java
authService.register(...)
```

с любым объектом типа `RegisterRequest`, mock должен вернуть заранее созданный `response`.

`any(RegisterRequest.class)` значит: нам не важно, какой именно объект `RegisterRequest` передали, важно только, что это `RegisterRequest`.

```java
mockMvc.perform(post("/auth/register")
```

Начинаем fake HTTP-запрос через MockMvc.

`post("/auth/register")` означает: сделать HTTP POST на endpoint `/auth/register`.

Настоящий сервер на порту `8081` не запускается. MockMvc выполняет запрос внутри тестового Spring context.

```java
        .contentType(MediaType.APPLICATION_JSON)
```

Говорим, что тело запроса имеет формат JSON.

Это как HTTP header:

```http
Content-Type: application/json
```

```java
        .content("""
```

Начинаем тело HTTP-запроса.

Тройные кавычки `"""` - это Java text block. Он удобен для многострочного JSON.

```json
{
  "email": "ivan.user@example.com",
  "password": "password123"
}
```

Это JSON, который как будто отправил клиент при регистрации.

```java
        """))
```

Закрываем text block, закрываем `.content(...)`, закрываем `mockMvc.perform(...)`.

```java
.andExpect(status().isCreated())
```

Проверяем HTTP status.

`isCreated()` означает status `201 Created`.

Такой status подходит для успешного создания нового ресурса.

```java
.andExpect(jsonPath("$.userId").value(1))
```

Проверяем JSON-ответ.

`jsonPath("$.userId")` значит: взять поле `userId` из корня JSON.

Ожидаем значение `1`.

```java
.andExpect(jsonPath("$.email").value("ivan.user@example.com"))
```

Проверяем, что поле `email` в JSON равно `"ivan.user@example.com"`.

```java
.andExpect(jsonPath("$.role").value("USER"))
```

Проверяем, что поле `role` в JSON равно `"USER"`.

Java enum `Role.USER` в JSON превращается в строку `"USER"`.

```java
.andExpect(jsonPath("$.accessToken").value("access-token"));
```

Проверяем, что поле `accessToken` равно `"access-token"`.

В этом тесте token не настоящий, потому что `AuthService` замокан.

```java
}
```

Конец тестового метода.

## AuthControllerTest.registerReturnsConflictWhenEmailAlreadyExists

```java
@Test
```

Метод ниже является тестом.

```java
void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
```

Название говорит: если email уже существует, регистрация должна вернуть конфликт.

В HTTP конфликт обычно обозначается status `409 Conflict`.

```java
when(authService.register(any(RegisterRequest.class)))
```

Настраиваем mock `AuthService`.

Если controller вызовет `authService.register(...)` с любым `RegisterRequest`, дальше будет задано поведение.

```java
        .thenThrow(new EmailAlreadyExistsException("ivan.user@example.com"));
```

Говорим mock-service: вместо успешного ответа выбрось exception.

Так мы имитируем ситуацию, когда бизнес-логика обнаружила duplicate email.

```java
mockMvc.perform(post("/auth/register")
```

Делаем fake POST-запрос на `/auth/register`.

```java
        .contentType(MediaType.APPLICATION_JSON)
```

Указываем, что request body - JSON.

```java
        .content("""
```

Начинаем JSON body.

```json
{
  "email": "ivan.user@example.com",
  "password": "password123"
}
```

Это валидный request. Ошибка здесь не из-за validation, а из-за того, что email уже занят.

```java
        """))
```

Закрываем JSON body и сам request.

```java
.andExpect(status().isConflict())
```

Проверяем HTTP status `409 Conflict`.

Этот status должен прийти не напрямую из controller, а через `GlobalExceptionHandler`.

```java
.andExpect(jsonPath("$.title").value("Email already exists"))
```

Проверяем поле `title` в ProblemDetail JSON.

`GlobalExceptionHandler` ставит этот title для `EmailAlreadyExistsException`.

```java
.andExpect(jsonPath("$.status").value(409))
```

Проверяем, что тело ответа тоже содержит status `409`.

```java
.andExpect(jsonPath("$.detail").value("Auth user with email ivan.user@example.com already exists"));
```

Проверяем подробное сообщение ошибки.

Оно приходит из `EmailAlreadyExistsException`.

```java
}
```

Конец тестового метода.

## AuthControllerTest.registerReturnsBadRequestWhenRequestIsInvalid

```java
@Test
```

Метод ниже является тестом.

```java
void registerReturnsBadRequestWhenRequestIsInvalid() throws Exception {
```

Название говорит: если request невалидный, controller должен вернуть `400 Bad Request`.

```java
mockMvc.perform(post("/auth/register")
```

Делаем fake POST-запрос на `/auth/register`.

В этом тесте мы не настраиваем `when(authService.register(...))`.

Причина: request не должен дойти до service. Validation должна остановить запрос раньше.

```java
        .contentType(MediaType.APPLICATION_JSON)
```

Указываем JSON request body.

```java
        .content("""
```

Начинаем JSON body.

```json
{
  "email": "not-email",
  "password": "short"
}
```

`email` не похож на email, поэтому нарушает `@Email`.

`password` слишком короткий, поэтому нарушает `@Size(min = 8, max = 72)`.

```java
        """))
```

Закрываем request body.

```java
.andExpect(status().isBadRequest())
```

Проверяем HTTP status `400 Bad Request`.

```java
.andExpect(jsonPath("$.title").value("Validation failed"))
```

Проверяем title ошибки.

Его ставит `GlobalExceptionHandler` в методе обработки `MethodArgumentNotValidException`.

```java
.andExpect(jsonPath("$.status").value(400))
```

Проверяем status внутри JSON body.

```java
.andExpect(jsonPath("$.detail").value("Request validation failed"))
```

Проверяем общее описание ошибки.

```java
.andExpect(jsonPath("$.errors.email").exists())
```

Проверяем, что в объекте `errors` есть ошибка для поля `email`.

Мы не проверяем точный текст, потому что текст validation-сообщения может зависеть от локали.

```java
.andExpect(jsonPath("$.errors.password").exists());
```

Проверяем, что в объекте `errors` есть ошибка для поля `password`.

```java
}
```

Конец тестового метода.

## AuthIntegrationTest.registerStoresAuthUserWithHashedPasswordInDatabase

```java
@Test
```

Метод ниже является тестом.

```java
void registerStoresAuthUserWithHashedPasswordInDatabase() throws Exception {
```

Название говорит: регистрация должна сохранить auth-пользователя в базе, и пароль должен быть hash, а не обычный текст.

Это integration test, поэтому здесь работает настоящий controller, настоящий service, настоящий repository и настоящий PostgreSQL test container.

```java
mockMvc.perform(post("/auth/register")
```

Делаем HTTP POST на `/auth/register` через MockMvc.

В отличие от `AuthControllerTest`, здесь нет mock `AuthService`. Запрос проходит через настоящие слои приложения.

```java
        .contentType(MediaType.APPLICATION_JSON)
```

Говорим, что request body - JSON.

```java
        .content("""
```

Начинаем JSON body.

```json
{
  "email": "ivan.user@example.com",
  "password": "password123"
}
```

Это данные для регистрации.

```java
        """))
```

Закрываем JSON body и request.

```java
.andExpect(status().isCreated())
```

Проверяем, что endpoint вернул `201 Created`.

```java
.andExpect(jsonPath("$.userId").isNumber())
```

Проверяем, что `userId` существует и является числом.

Мы не проверяем конкретное значение `1`, потому что id генерирует база. В integration test лучше не привязываться к конкретному id, если это не важно для смысла теста.

```java
.andExpect(jsonPath("$.email").value("ivan.user@example.com"))
```

Проверяем email в response JSON.

```java
.andExpect(jsonPath("$.role").value("USER"))
```

Проверяем, что обычная регистрация дала роль `USER`.

```java
.andExpect(jsonPath("$.accessToken").isString());
```

Проверяем, что response содержит `accessToken`, и это строка.

Здесь token уже настоящий, потому что работает настоящий `JwtService`.

```java
var authUser = authUserRepository.findByEmail("ivan.user@example.com").orElseThrow();
```

После HTTP-запроса напрямую обращаемся к repository.

Ищем пользователя в настоящей тестовой PostgreSQL-базе по email.

`var` значит: Java сама выводит тип переменной. Здесь тип будет `AuthUser`.

`orElseThrow()` значит: если пользователь не найден, выбросить exception и провалить тест.

```java
assertTrue(passwordEncoder.matches("password123", authUser.getPasswordHash()));
```

Проверяем главный security-смысл регистрации.

`authUser.getPasswordHash()` берёт hash, который был сохранён в базе.

`passwordEncoder.matches("password123", hash)` проверяет, что обычный пароль соответствует hash.

Если бы в базе лежал неправильный hash или обычный пароль в неожиданном формате, проверка бы упала.

```java
}
```

Конец тестового метода.
