# Backend Services Integration Plan

## Цель

Собрать текущие сервисы магазина в один последовательный backend flow:

```text
auth-service выпускает JWT
gateway-service проверяет JWT
gateway-service прокидывает X-User-* headers
catalog-service публично отдает товары на чтение
cart-service работает только для authenticated user
order-service оформляет заказ для authenticated user
```

Корзина без логина отложена в [further-plans.md](further-plans.md). В ближайшем плане она не
реализуется.

## Правила работы с планом

- Не добавлять default-значения в config properties без отдельного решения.
- Не коммитить изменения, пока это явно не попрошено.
- Если в рабочем дереве есть чужие изменения, не откатывать их.
- Сначала закрывать runtime-проверки текущего этапа, потом переходить к следующему.

## Фактическое состояние репозитория

### auth-service

Готово:

- `POST /auth/register`;
- `POST /auth/login`;
- `GET /auth/me`;
- JWT с claims:

```text
sub   = auth_users.id
email = auth_users.email
role  = auth_users.role
```

Важно:

- регистрация создает запись только в `auth_users`;
- профиль в `user-service` при регистрации не создается.

### gateway-service

Готово:

- routes для `auth`, `user`, `catalog`, `cart`, `order`, `payment`;
- актуальный Spring Cloud Gateway prefix:

```yaml
spring.cloud.gateway.server.webflux.routes
```

- JWT validation через `AUTH_JWT_SECRET`;
- удаление клиентских `X-User-*` headers;
- добавление trusted headers после проверки JWT:

```http
X-User-Id
X-User-Email
X-User-Role
```

Текущее security-решение:

```text
POST /api/auth/register                 public
POST /api/auth/login                    public
GET  /api/catalog/products              public
GET  /api/catalog/products/**           public
GET  /api/catalog/categories            public
GET  /api/catalog/categories/**         public
everything else                         authenticated
```

### catalog-service

Готово:

- `/products`;
- `/products/{id}`;
- `/categories`;
- create/update/delete товаров;
- create categories;
- PostgreSQL + Liquibase.

Ближайшая политика:

- чтение каталога public через gateway;
- изменения каталога позже ограничить ролью `ADMIN`.

### user-service

Текущее состояние:

- `/users`;
- `/users/{id}`;
- `/users/by-email`;
- `PUT /users/{id}`;
- таблица `users` имеет свой независимый `id`.

Проблема:

```text
auth_users.id != users.id
```

Gateway кладет в `X-User-Id` именно `auth_users.id`, а `user-service` сейчас работает со своим
`users.id`.

### cart-service

Текущее состояние:

- endpoints принимают `userId` из body/path;
- Redis key фактически строится из request userId;
- cart уже проверяет товар через `catalog-service`.

Проблема:

```text
authenticated user может передать чужой userId
```

Цель:

- убрать `userId` из body/path;
- брать user id из trusted `X-User-Id`.

### order-service

Текущее состояние:

- controller уже берет `X-User-Id`;
- `OrderCreateRequest` уже не содержит `userId`;
- get order проверяет владельца по `X-User-Id`.

Проблема:

- клиент все еще передает `productName`;
- клиент все еще передает `productPrice`;
- order-service пока не оформляет заказ из корзины;
- order-service пока не берет актуальную цену из catalog-service.

## Ближайший целевой внешний API

Public:

```http
POST /api/auth/register
POST /api/auth/login

GET /api/catalog/products
GET /api/catalog/products/{id}
GET /api/catalog/categories
```

Authenticated:

```http
GET /api/auth/me

POST /api/users/me
GET  /api/users/me
PUT  /api/users/me

POST   /api/cart/items
GET    /api/cart
PUT    /api/cart/items/{productId}
DELETE /api/cart/items/{productId}
DELETE /api/cart

POST /api/orders/checkout
GET  /api/orders/{id}
GET  /api/orders
```

Admin/future:

```http
POST   /api/catalog/products
PUT    /api/catalog/products/{id}
DELETE /api/catalog/products/{id}
POST   /api/catalog/categories
```

## Этап 0. Runtime readiness

Перед ручными проверками должны быть запущены:

```text
auth-postgres
catalog-postgres
user-postgres
order-postgres
cart-redis

auth-service       8081
user-service       8082
catalog-service    8083
cart-service       8084
order-service      8085
gateway-service    8080
```

Проверки портов:

```powershell
Test-NetConnection localhost -Port 8080
Test-NetConnection localhost -Port 8081
Test-NetConnection localhost -Port 8082
Test-NetConnection localhost -Port 8083
Test-NetConnection localhost -Port 8084
Test-NetConnection localhost -Port 8085
```

Критерий готовности:

- нужные сервисы стартуют;
- `AUTH_JWT_SECRET` задан одинаково для `auth-service` и `gateway-service`;
- gateway стартует без ошибок JWT decoder.

## Этап 1. Проверить auth и gateway JWT

Цель: доказать, что auth-service и gateway работают как единая security boundary.

Проверить register:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/auth/register" `
  -H "Content-Type: application/json" `
  -d '{"email":"ivan.user@example.com","password":"password123"}'
```

Проверить login:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"email":"ivan.user@example.com","password":"password123"}'
```

Проверить protected endpoint без JWT:

```powershell
curl.exe -i "http://localhost:8080/api/auth/me"
```

Ожидаемо:

```text
401 Unauthorized
```

Проверить protected endpoint с JWT:

```powershell
curl.exe -i "http://localhost:8080/api/auth/me" `
  -H "Authorization: Bearer <accessToken>"
```

Критерии готовности:

- register/login доступны без JWT;
- login возвращает `accessToken`;
- `/api/auth/me` без JWT возвращает `401`;
- `/api/auth/me` с JWT возвращает текущего пользователя;
- gateway и auth используют один secret.

## Этап 2. Проверить public catalog read через gateway

Цель: подтвердить выбранный UX: каталог можно смотреть без логина.

Проверки:

```powershell
curl.exe -i "http://localhost:8080/api/catalog/products"
curl.exe -i "http://localhost:8080/api/catalog/products/1"
curl.exe -i "http://localhost:8080/api/catalog/categories"
```

Ожидаемо:

```text
200 OK без Authorization header
```

Негативные проверки:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/cart/items" `
  -H "Content-Type: application/json" `
  -d '{"userId":1,"productId":1,"quantity":1}'
```

Ожидаемо:

```text
401 Unauthorized
```

Критерии готовности:

- catalog read endpoints доступны без JWT;
- cart без JWT закрыт;
- orders без JWT закрыты;
- users без JWT закрыты.

## Этап 3. Принять модель связи auth-service и user-service

Проблема:

```text
JWT sub = auth_users.id
user-service сейчас работает с users.id
```

Решение для ближайшего плана:

```text
auth_users.id становится главным external user id
user-service добавляет поле auth_user_id
```

Целевое изменение в user DB:

```sql
alter table users add column auth_user_id bigint;
alter table users add constraint users_auth_user_id_unique unique (auth_user_id);
```

Для новых данных `auth_user_id` должен быть `not null`. Если в локальной базе уже есть старые
users, миграцию нужно делать аккуратно: сначала nullable, заполнить данные, потом добавить not null.

Целевой API:

```http
POST /api/users/me
GET  /api/users/me
PUT  /api/users/me
```

Правило:

- `authUserId` брать из `X-User-Id`;
- email брать из `X-User-Email`;
- не доверять email/userId из request body.

Критерии готовности:

- в user-service есть `auth_user_id`;
- `/users/me` создает профиль текущего auth user;
- `/users/me` возвращает профиль текущего auth user;
- `/users/me` обновляет только профиль текущего auth user.

## Этап 4. Перевести cart-service на X-User-Id

Текущий API:

```http
POST /cart/items
GET  /cart/{userId}
PUT  /cart/{userId}/items/{productId}
DELETE /cart/{userId}/items/{productId}
DELETE /cart/{userId}
```

Целевой API:

```http
POST   /cart/items
GET    /cart
PUT    /cart/items/{productId}
DELETE /cart/items/{productId}
DELETE /cart
```

Что изменить:

- убрать `userId` из `AddCartItemRequest`;
- controller должен читать `@RequestHeader("X-User-Id") Long userId`;
- service может продолжить принимать `userId` параметром, но он должен приходить из trusted header;
- Redis key оставить в форме `cart:{userId}` или переименовать в `cart:user:{userId}`.

Критерии готовности:

- cart нельзя открыть или изменить без JWT через gateway;
- body/path больше не содержит userId;
- cart работает по trusted `X-User-Id`;
- cart продолжает проверять товар через `catalog-service`;
- cart tests обновлены под новый API.

## Этап 5. Перевести order-service на checkout из cart

Текущее хорошее состояние:

- order controller уже берет `X-User-Id`;
- userId уже убран из `OrderCreateRequest`;
- получение заказа проверяет владельца.

Оставшаяся проблема:

```text
client все еще присылает productName/productPrice
```

Целевой API:

```http
POST /orders/checkout
```

Тело запроса на первом этапе может быть пустым.

Целевой flow:

```text
1. order-service берет userId из X-User-Id.
2. order-service получает cart текущего пользователя из cart-service.
3. order-service проверяет, что cart не пустой.
4. order-service получает актуальные продукты из catalog-service.
5. order-service берет productName/productPrice только из catalog-service.
6. order-service сохраняет order snapshot.
7. order-service очищает cart.
```

Нужные internal clients:

```text
order-service -> cart-service
order-service -> catalog-service
```

Критерии готовности:

- checkout не принимает productName/productPrice от клиента;
- checkout строит order из cart;
- цена берется из catalog;
- после checkout cart очищается;
- ошибки downstream-сервисов мапятся в понятные `ProblemDetail`.

## Этап 6. Role policy для catalog writes

Сейчас gateway проверяет только authentication.

Цель:

```text
GET catalog public
POST/PUT/DELETE catalog ADMIN only
```

Можно реализовать:

- на gateway уровне через role claims;
- в catalog-service через `X-User-Role`;
- комбинированно.

Для текущего проекта достаточно:

```text
gateway authenticates
catalog-service authorizes writes by X-User-Role
```

Критерии готовности:

- обычный USER не может создавать/изменять/удалять товары;
- ADMIN может создавать/изменять/удалять товары;
- GET catalog остается public.

## Этап 7. Payment-service

Payment делать после checkout.

Будущий flow:

```text
order CREATED
payment request
payment success/failure
order status update
```

Пока payment-service не блокирует интеграцию auth/catalog/cart/order.

## Тестовая стратегия

### Gateway

- register/login public;
- catalog GET public;
- cart/order/user protected;
- invalid JWT gives `401`;
- valid JWT reaches downstream;
- client-supplied `X-User-*` headers are stripped;
- gateway adds trusted `X-User-*`.

### User

- `/users/me` create/get/update;
- duplicate profile for same `auth_user_id` rejected;
- email comes from `X-User-Email`.

### Cart

- cart uses `X-User-Id`;
- request body no longer contains `userId`;
- product validation via catalog still works;
- not enough quantity returns `409`;
- catalog unavailable returns `503`.

### Order

- order uses `X-User-Id`;
- user cannot read another user's order;
- checkout uses cart;
- checkout gets price/name from catalog;
- checkout clears cart.

## Рекомендуемый порядок выполнения

Текущий practical status:

```text
Этап 0  not verified
Этап 1  implemented in code, not runtime-verified
Этап 2  implemented in code, not runtime-verified
Этап 3  not started
Этап 4  not started
Этап 5  partially started in order-service, checkout not implemented
Этап 6  not started
Этап 7  future
```

Следующий правильный шаг: поднять runtime и закрыть проверки этапов 0-2.

1. Runtime readiness.
2. Проверить auth/gateway JWT.
3. Проверить public catalog read и protected cart/order/user.
4. Реализовать связь `auth_users` и `users` через `auth_user_id`.
5. Добавить `/users/me`.
6. Перевести cart на `X-User-Id`.
7. Перевести order на checkout из cart/catalog.
8. Добавить role policy для catalog writes.
9. Потом переходить к payment.
