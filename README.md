# LinkTracker

LinkTracker — это проект для отслеживания обновлений по ссылкам с Telegram-ботом и отдельным
сервисом scrapper.

## Что входит в проект

- `bot` — Telegram-бот на Spring Boot. Принимает команды пользователя и отправляет их в scrapper.
- `scrapper` — HTTP-сервис, который:
  - хранит зарегистрированные чаты и подписки,
  - умеет работать в режимах `SQL` и `ORM`,
  - применяет миграции Flyway,
  - периодически опрашивает отслеживаемые ссылки и отправляет обновления в `bot`.
- `contract` — общие DTO и API-контракты.
- `compose.yaml` — локальный PostgreSQL для разработки.

## Основные возможности

- Команды бота:
  - `/start`
  - `/help`
  - `/track`
  - `/list`
  - `/untrack <ссылка>`
- Регистрация Telegram-чата в scrapper.
- Хранение подписок и тегов в PostgreSQL.
- Два режима доступа к БД в scrapper:
  - `SQL` — через `JdbcClient`
  - `ORM` — через JPA / Hibernate
- Миграции Flyway.
- Периодический polling обновлений ссылок.

## Требования

- Java 25. `pom.xml` требует `java.version=25`.
- Maven 3.9.12 или новее. `pom.xml` требует `maven.version=3.9.12`.
- Docker / Docker Compose — если вы хотите быстро поднять PostgreSQL локально через `compose.yaml`.
- Telegram bot token от [@BotFather](https://t.me/BotFather).

## Архитектура и порты

По умолчанию сервисы запускаются так:

- `bot` — `http://localhost:8080`. `bot/src/main/resources/application.yaml`.
- `scrapper` — `http://localhost:8081`. `scrapper/src/main/resources/application.yaml`.
- PostgreSQL — `localhost:5432`, база `linktracker`, пользователь `postgres`, пароль `postgres`. `compose.yaml`.

Связи между сервисами:

- `bot` отправляет запросы в `scrapper` по адресу `http://localhost:8081`. `bot/src/main/resources/application.yaml`.
- `scrapper` отправляет уведомления в `bot` по адресу `http://localhost:8080/updates`. `scrapper/src/main/resources/application.yaml`. `bot/src/main/java/backend/academy/linktracker/bot/api/BotUpdatesController.java`.

## Переменные окружения

### Обязательные

#### Для `bot`

- `TELEGRAM_TOKEN` — токен Telegram-бота. `bot/src/main/resources/application.yaml`.

#### Для `scrapper`

Для базового запуска на локальном PostgreSQL обязательных секретов нет, если вы не используете
GitHub / StackOverflow API с авторизацией. Но БД должна быть доступна.

### Необязательные

#### Для `scrapper`

- `SCRAPPER_DATASOURCE_URL` — JDBC URL базы данных.
- `SCRAPPER_DATASOURCE_USERNAME` — пользователь БД.
- `SCRAPPER_DATASOURCE_PASSWORD` — пароль БД.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` —
  альтернативные стандартные Spring-переменные datasource.
- `SCRAPPER_ACCESS_TYPE` — режим доступа к БД:
  - `SQL` — по умолчанию,
  - `ORM` — JPA / Hibernate.
- `GITHUB_TOKEN` — GitHub token для более комфортной работы с GitHub API.
- `STACKOVERFLOW_KEY` — StackOverflow API key.
- `STACKOVERFLOW_ACCESS_KEY` — StackOverflow access token.

Значения и дефолты указаны здесь. `scrapper/src/main/resources/application.yaml`.

## Быстрый запуск

### Вариант 1. Локально через Docker Compose + запуск приложений из IDE / Maven

#### 1. Поднимите PostgreSQL

```bash
docker compose up -d postgres
```

Конфигурация базы описана в `compose.yaml`. `compose.yaml`.

#### 2. Задайте переменные окружения

Минимум нужен Telegram token:

```bash
export TELEGRAM_TOKEN=1234512345:ABCDEFG...
```

Если используете локальный `compose.yaml`, datasource можно не задавать: по умолчанию scrapper
подключается к `jdbc:postgresql://localhost:5432/linktracker`. `scrapper/src/main/resources/application.yaml`.

При необходимости можно явно указать режим хранения:

```bash
export SCRAPPER_ACCESS_TYPE=SQL
```

или

```bash
export SCRAPPER_ACCESS_TYPE=ORM
```

#### 3. Запустите `scrapper`

Из IDE:

- main class: `backend.academy.linktracker.scrapper.ScrapperApplication`. `scrapper/src/main/java/backend/academy/linktracker/scrapper/ScrapperApplication.java`.

Через Maven:

```bash
mvn -pl scrapper spring-boot:run
```

#### 4. Запустите `bot`

Из IDE:

- main class: `backend.academy.linktracker.bot.BotApplication`. `bot/src/main/java/backend/academy/linktracker/bot/BotApplication.java`.

Через Maven:

```bash
mvn -pl bot spring-boot:run
```

#### 5. Откройте Telegram и используйте бота

Базовый сценарий:

1. `/start`
2. `/help`
3. `/track`
4. отправьте ссылку
5. отправьте теги через запятую или `-`
6. `/list`
7. `/untrack <ссылка>`

Команды реализованы здесь:

- `StartCommand`. `bot/src/main/java/backend/academy/linktracker/bot/command/impl/StartCommand.java`.
- `HelpCommand`. `bot/src/main/java/backend/academy/linktracker/bot/command/impl/HelpCommand.java`.
- `TrackCommand`. `bot/src/main/java/backend/academy/linktracker/bot/command/impl/TrackCommand.java`.
- `ListCommand`. `bot/src/main/java/backend/academy/linktracker/bot/command/impl/ListCommand.java`.
- `UntrackCommand`. `bot/src/main/java/backend/academy/linktracker/bot/command/impl/UntrackCommand.java`.

### Вариант 2. Полностью через IDE

1. Поднимите PostgreSQL через `compose.yaml`:

```bash
docker compose up -d postgres
```

2. Создайте две Run Configuration:
   - `ScrapperApplication`
   - `BotApplication`
3. В `Environment variables` добавьте хотя бы:

```text
TELEGRAM_TOKEN=1234512345:ABCDEFG...
SCRAPPER_ACCESS_TYPE=SQL
```

4. Сначала запускайте `scrapper`, потом `bot`.

## Режимы доступа к БД в scrapper

### `SQL`

- Используются SQL-репозитории на `JdbcClient`.
- Это режим по умолчанию. `scrapper/src/main/resources/application.yaml`.

Включение:

```bash
export SCRAPPER_ACCESS_TYPE=SQL
```

### `ORM`

- Используются JPA-репозитории и Hibernate.
- Подходит для проверки ORM-реализации поверх той же PostgreSQL схемы.

Включение:

```bash
export SCRAPPER_ACCESS_TYPE=ORM
```

ORM-конфигурация находится здесь. `scrapper/src/main/java/backend/academy/linktracker/scrapper/configuration/OrmPersistenceConfiguration.java`.

## Как работают миграции

- SQL миграции лежат в `scrapper/src/main/resources/db/migration`.
- Основная миграция схемы — `V1__create_scrapper_schema.sql`. `scrapper/src/main/resources/db/migration/V1__create_scrapper_schema.sql`.
- При запуске scrapper создаётся `Flyway` bean, который вызывает `migrate()` на старте. `scrapper/src/main/java/backend/academy/linktracker/scrapper/configuration/FlywayConfiguration.java`.

То есть порядок ожидается такой:

1. поднимается `DataSource`,
2. запускается Flyway,
3. применяются миграции,
4. после этого стартуют SQL/ORM-компоненты scrapper.

## HTTP API

### Scrapper

Основные endpoint'ы:

- `POST /tg-chat/{id}` — зарегистрировать чат. `scrapper/src/main/java/backend/academy/linktracker/scrapper/api/controller/TgChatController.java`.
- `DELETE /tg-chat/{id}` — удалить чат. `scrapper/src/main/java/backend/academy/linktracker/scrapper/api/controller/TgChatController.java`.
- `GET /links` — получить список отслеживаемых ссылок. `scrapper/src/main/java/backend/academy/linktracker/scrapper/api/controller/LinkController.java`.
- `POST /links` — добавить ссылку. `scrapper/src/main/java/backend/academy/linktracker/scrapper/api/controller/LinkController.java`.
- `DELETE /links` — удалить ссылку. `scrapper/src/main/java/backend/academy/linktracker/scrapper/api/controller/LinkController.java`.

### Bot

- `POST /updates` — endpoint, куда scrapper отправляет уведомления об обновлениях. `bot/src/main/java/backend/academy/linktracker/bot/api/BotUpdatesController.java`.

## Swagger / OpenAPI

В проекте подключён springdoc:

- `bot/pom.xml`
- `scrapper/pom.xml`

По умолчанию можно ожидать стандартные endpoint'ы:

- `/v3/api-docs`
- `/swagger-ui.html`

Если вы не хотите держать их включёнными в production, отключите их через свойства springdoc.

## Полезные команды

### Поднять PostgreSQL

```bash
docker compose up -d postgres
```

### Остановить PostgreSQL

```bash
docker compose down
```

### Запустить bot

```bash
mvn -pl bot spring-boot:run
```

### Запустить scrapper

```bash
mvn -pl scrapper spring-boot:run
```

### Запустить все тесты

```bash
mvn test
```

### Запустить только тесты scrapper

```bash
mvn -pl scrapper test
```

## Что проверить, если проект не стартует

### `bot` не запускается

Проверьте:

- задан ли `TELEGRAM_TOKEN`,
- запущен ли `scrapper` на `localhost:8081`.

### `scrapper` не запускается

Проверьте:

- доступна ли PostgreSQL,
- совпадают ли `SCRAPPER_DATASOURCE_URL` / логин / пароль,
- не занят ли порт `8081`,
- правильно ли выставлен `SCRAPPER_ACCESS_TYPE`.

### В SQL / ORM режиме не создаются таблицы

Проверьте:

- что scrapper стартует именно с тем datasource, который вы ожидаете,
- что PostgreSQL доступен,
- что Flyway миграции лежат в `classpath:db/migration`,
- что используется актуальный `FlywayConfiguration`. `scrapper/src/main/java/backend/academy/linktracker/scrapper/configuration/FlywayConfiguration.java`.

## Замечания

- По умолчанию scrapper poll'ит ссылки раз в `60s`. `scrapper/src/main/resources/application.yaml`.
- Размер batch polling'а задаётся через `app.persistence.polling-batch-size`. `scrapper/src/main/resources/application.yaml`.
- `bot` и `scrapper` обмениваются сообщениями по HTTP, поэтому для локального запуска оба сервиса должны быть подняты одновременно.
