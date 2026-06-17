# LinkTracker

Проект выполнен в рамках второго семестра курса Т-академии backend 2025-2026.

LinkTracker - сервисный backend-проект для отслеживания обновлений по ссылкам. Пользователь управляет подписками через Telegram-бота, `scrapper` периодически проверяет внешние источники, `ai-agent` фильтрует, группирует и кратко пересказывает найденные обновления, а итоговые уведомления доставляются обратно в Telegram.

## Что Входит В Проект

- `bot` - Telegram-бот на Spring Boot. Обрабатывает команды пользователя, вызывает Scrapper API, принимает HTTP-уведомления или читает обработанные события из Kafka.
- `scrapper` - сервис отслеживания ссылок. Хранит чаты, ссылки, подписки, теги и фильтры, опрашивает GitHub и Stack Overflow, публикует найденные обновления.
- `ai-agent` - сервис обработки обновлений. Читает raw-события из Kafka, применяет фильтрацию, приоритизацию, группировку и суммаризацию через Gemini API или fallback-реализацию.
- `contract` - общий модуль с HTTP-контрактами, DTO и Avro-схемой `LinkUpdateEvent`.
- `migrations` - SQL-миграции Flyway для схемы Scrapper и outbox-таблиц.
- `prometheus`, `grafana`, `example_pql.txt`, `OBSERVABILITY.md` - конфигурация наблюдаемости, PromQL-запросы, дашборды и алертинг.
- `compose.yaml` - локальная инфраструктура и контейнерный запуск основных сервисов.

## Основные Возможности

- Команды Telegram-бота: `/start`, `/help`, `/track`, `/list`, `/untrack <ссылка>`, `/cancel`.
- Диалоговое добавление ссылки с тегами.
- Поддержка ссылок GitHub repositories и Stack Overflow questions.
- Регистрация и удаление Telegram-чатов через Scrapper API.
- Хранение данных в PostgreSQL.
- Два режима доступа к БД в `scrapper`: `SQL` через `JdbcClient` и `ORM` через JPA/Hibernate.
- Миграции Flyway.
- Отдельный API для работы с тегами подписок.
- Периодический polling обновлений ссылок.
- Асинхронная доставка обновлений через Kafka.
- JSON/Avro-сериализация событий и Schema Registry.
- DLQ для неуспешно обработанных Kafka-сообщений в `bot`.
- Transactional Outbox в `scrapper` для надежной публикации Kafka-событий.
- Кэширование списка ссылок через Valkey cluster и локальный client-side cache.
- Rate limiting Scrapper API через Bucket4j.
- Retry и Circuit Breaker для внешних HTTP-вызовов через Resilience4j.
- AI-обработка обновлений: фильтрация, приоритизация, группировка и суммаризация.
- Метрики Prometheus, Grafana dashboards, Pushgateway и Grafana Alert.
- Dockerfile для `bot` и `scrapper`, локальная сборка через Docker Compose.

## Используемые Технологии

- Java 25, Maven 3.9.12.
- Spring Boot 4, Spring WebMVC, Spring Validation, Spring Actuator.
- Spring Data JDBC, Spring Data JPA, Hibernate.
- PostgreSQL 17, Flyway.
- Apache Kafka 3-брокерный KRaft-кластер, Confluent Schema Registry, Avro.
- Valkey 8 cluster, Spring Data Redis, Caffeine.
- Resilience4j, Bucket4j.
- Telegram Bot API через `java-telegram-bot-api`.
- Micrometer, Prometheus, Pushgateway, Grafana.
- Docker, Docker Compose.
- OpenAPI / Swagger UI через springdoc.
- JUnit 5, Testcontainers, WireMock, Awaitility, JMeter.
- Spotless, PMD, SpotBugs, JaCoCo, Modernizer.

## Архитектура

Основной асинхронный поток обновлений:

1. `scrapper` poll'ит GitHub и Stack Overflow.
2. Новые обновления сохраняются в outbox и публикуются в Kafka topic `link.raw-updates`.
3. `ai-agent` читает `link.raw-updates`, фильтрует и обогащает сообщения.
4. `ai-agent` публикует результат в `link.processed-updates`.
5. `bot` читает `link.processed-updates`, дедуплицирует сообщения и отправляет уведомления в Telegram.

Для упрощенного сценария можно использовать HTTP-транспорт: `scrapper` отправляет уведомления напрямую в `bot` на `POST /updates`.

## Модули И Порты

По умолчанию используются такие адреса:

- `bot` - `http://localhost:8080`, management/metrics - `http://localhost:8011/metrics`.
- `scrapper` - `http://localhost:8081`, management/metrics - `http://localhost:8012/metrics`.
- `ai-agent` - `http://localhost:8082`.
- PostgreSQL - `localhost:5433`, база `linktracker`, пользователь `postgres`, пароль `postgres`.
- Kafka - `localhost:9092`, `localhost:9093`, `localhost:9094`.
- Schema Registry - `http://localhost:8085`.
- Valkey cluster - `localhost:6380` ... `localhost:6385`.
- Pushgateway - `http://localhost:9091`.
- Prometheus - `http://localhost:9090`.
- Grafana - `http://localhost:3000`, логин/пароль по умолчанию `admin` / `admin`.

Важно: в `compose.yaml` сейчас контейнеризованы `bot`, `scrapper` и инфраструктура. `ai-agent` запускается отдельной Maven-командой или из IDE, если нужен полный AI-пайплайн.

## Переменные Окружения

### Обязательные

- `TELEGRAM_TOKEN` - токен Telegram-бота от [@BotFather](https://t.me/BotFather).
- `GITHUB_TOKEN` - токен GitHub API для `scrapper`.
- `STACKOVERFLOW_KEY` - Stack Overflow API key.
- `STACKOVERFLOW_ACCESS_KEY` - Stack Overflow access token.

Для локального smoke-запуска через `compose.yaml` у GitHub/Stack Overflow переменных есть dev-заглушки, но для реальной проверки обновлений лучше задавать настоящие значения.

### Scrapper

- `SCRAPPER_DATASOURCE_URL` - JDBC URL PostgreSQL.
- `SCRAPPER_DATASOURCE_USERNAME` - пользователь БД.
- `SCRAPPER_DATASOURCE_PASSWORD` - пароль БД.
- `SCRAPPER_ACCESS_TYPE` - режим доступа к БД: `ORM` по умолчанию, либо `SQL`.
- `SCRAPPER_POLLING_ENABLED` - включает polling ссылок, по умолчанию `false`.
- `SCRAPPER_NOTIFICATION_TRANSPORT` - транспорт уведомлений: `KAFKA` по умолчанию, либо `HTTP`.
- `SCRAPPER_KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers.
- `SCRAPPER_KAFKA_UPDATES_TOPIC` - topic raw-обновлений, по умолчанию `link.raw-updates`.
- `SCRAPPER_KAFKA_DIRECT_PUBLISHER_ENABLED` - включает прямую Kafka-публикацию вместо outbox, по умолчанию `false`.
- `SCRAPPER_KAFKA_OUTBOX_DISPATCH_INTERVAL` - период отправки outbox-событий, по умолчанию `1s`.
- `SCRAPPER_KAFKA_OUTBOX_MAX_ATTEMPTS` - максимум попыток outbox-события, по умолчанию `5`.
- `SCRAPPER_KAFKA_OUTBOX_BATCH_SIZE` - размер пачки outbox dispatcher, по умолчанию `100`.
- `SCRAPPER_KAFKA_SCHEMA_REGISTRY_URL` - URL Schema Registry.
- `SCRAPPER_VALKEY_CACHE_ENABLED` - включает кэш списка ссылок, по умолчанию `true`.
- `SCRAPPER_VALKEY_CLUSTER_NODES` - ноды Valkey cluster.
- `SCRAPPER_VALKEY_CACHE_TTL` - TTL кэша ссылок, по умолчанию `10m`.
- `SCRAPPER_RATE_LIMIT_ENABLED` - включает rate limiting, по умолчанию `true`.
- `SCRAPPER_RATE_LIMIT_CAPACITY`, `SCRAPPER_RATE_LIMIT_REFILL_TOKENS`, `SCRAPPER_RATE_LIMIT_REFILL_PERIOD` - параметры лимита запросов.
- `SCRAPPER_PUSHGATEWAY_ENABLED` - включает push метрик в Pushgateway.
- `SCRAPPER_MANAGEMENT_PORT` - порт management endpoint'ов, по умолчанию `8012`.

### Bot

- `BOT_SCRAPPER_BASE_URL` - адрес Scrapper API.
- `BOT_NOTIFICATION_TRANSPORT` - прием уведомлений: `KAFKA` по умолчанию, либо `HTTP`.
- `BOT_KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers.
- `BOT_KAFKA_UPDATES_TOPIC` - topic обработанных обновлений, по умолчанию `link.processed-updates`.
- `BOT_KAFKA_GROUP_ID` - consumer group ID.
- `BOT_KAFKA_SCHEMA_REGISTRY_URL` - URL Schema Registry.
- `BOT_KAFKA_DLQ_TOPIC` - DLQ topic, по умолчанию `link-updates-dlq`.
- `BOT_KAFKA_MAX_ATTEMPTS` - число попыток бизнес-обработки перед DLQ, по умолчанию `3`.
- `BOT_KAFKA_IDEMPOTENCY_CACHE_SIZE` - размер in-memory кэша id сообщений, по умолчанию `10000`.
- `BOT_PUSHGATEWAY_ENABLED` - включает push метрик в Pushgateway.
- `BOT_MANAGEMENT_PORT` - порт management endpoint'ов, по умолчанию `8011`.

### AI Agent

- `AI_AGENT_KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers.
- `AI_AGENT_KAFKA_RAW_UPDATES_TOPIC` - входной topic, по умолчанию `link.raw-updates`.
- `AI_AGENT_KAFKA_PROCESSED_UPDATES_TOPIC` - выходной topic, по умолчанию `link.processed-updates`.
- `AI_AGENT_KAFKA_GROUP_ID` - consumer group ID.
- `AI_AGENT_KAFKA_SCHEMA_REGISTRY_URL` - URL Schema Registry.
- `AI_AGENT_SUMMARIZATION_PROVIDER` - провайдер суммаризации, по умолчанию `api`.
- `AI_AGENT_SUMMARIZATION_API_BASE_URL` - base URL Gemini API.
- `AI_AGENT_SUMMARIZATION_API_TOKEN` - API token для Gemini.
- `AI_AGENT_SUMMARIZATION_API_MODEL` - модель, по умолчанию `gemini-3.5-flash`.
- `AI_AGENT_SUMMARIZATION_API_PROMPT` - prompt для суммаризации.
- `AI_AGENT_GROUPING_WINDOW_MS` - окно группировки обновлений, по умолчанию `30000`.

## Быстрый Запуск

### Инфраструктура И Основные Сервисы

```bash
docker compose up -d --build
```

Команда поднимает PostgreSQL, Valkey cluster, Kafka, Schema Registry, `scrapper`, `bot`, Pushgateway, Prometheus и Grafana.

Минимально перед запуском стоит задать токен бота:

```bash
export TELEGRAM_TOKEN=1234512345:ABCDEFG...
```

На Windows PowerShell:

```powershell
$env:TELEGRAM_TOKEN = "1234512345:ABCDEFG..."
docker compose up -d --build
```

### AI Agent

Если нужен полный Kafka-пайплайн `scrapper -> ai-agent -> bot`, запустите `ai-agent` отдельно:

```bash
mvn -pl ai-agent spring-boot:run
```

Для суммаризации через API задайте:

```bash
export AI_AGENT_SUMMARIZATION_API_TOKEN=...
```

Без токена сервис использует fallback-суммаризацию там, где это предусмотрено кодом.

### Запуск Из IDE / Maven

Поднимите инфраструктуру:

```bash
docker compose up -d postgres kafka-1 kafka-2 kafka-3 schema-registry kafka-topics-init valkey-cluster-1 valkey-cluster-2 valkey-cluster-3 valkey-cluster-4 valkey-cluster-5 valkey-cluster-6 valkey-cluster-init
```

Затем запустите приложения:

```bash
mvn -pl scrapper spring-boot:run
mvn -pl ai-agent spring-boot:run
mvn -pl bot spring-boot:run
```

Если Valkey для локального запуска не нужен, перед стартом `scrapper` можно отключить кэш:

```bash
export SCRAPPER_VALKEY_CACHE_ENABLED=false
```

Основные main-классы:

- `backend.academy.linktracker.scrapper.ScrapperApplication`.
- `backend.academy.linktracker.ai.AiAgentApplication`.
- `backend.academy.linktracker.bot.BotApplication`.

## HTTP API

### Scrapper

- `POST /tg-chat/{id}` - зарегистрировать чат.
- `DELETE /tg-chat/{id}` - удалить чат.
- `GET /links` - получить список отслеживаемых ссылок.
- `POST /links` - добавить ссылку.
- `DELETE /links` - удалить ссылку.
- `GET /tags` - получить теги отслеживаемой ссылки.
- `POST /tags` - добавить тег к подписке.
- `PUT /tags` - заменить набор тегов у подписки.
- `DELETE /tags` - удалить тег у подписки.

### Bot

- `POST /updates` - HTTP endpoint для уведомлений от `scrapper`, если выбран `BOT_NOTIFICATION_TRANSPORT=HTTP`.

Swagger UI доступен у web-сервисов по стандартным endpoint'ам:

- `/v3/api-docs`
- `/swagger-ui.html`

## Kafka И Avro

- `link.raw-updates` - raw-обновления от `scrapper`.
- `link.processed-updates` - обработанные обновления от `ai-agent` для `bot`.
- `link-updates-dlq` - DLQ для сообщений, которые `bot` не смог обработать.

Схема события хранится в `contract/src/main/resources/avro/LinkUpdateEvent.avsc`. При Avro-режиме используется Confluent wire format и Schema Registry на `http://localhost:8085`.

Kafka topics создаются через `kafka-topics-init` с `partitions=3`, `replication-factor=3`, `min.insync.replicas=2`. Это позволяет параллелить обработку и переживать отказ одного брокера при `acks=all`.

## Кэширование И Rate Limiting

`scrapper` использует Valkey cluster для кэширования результатов `GET /links`. Дополнительно включен client-side cache с ограничением размера. Для локальной разработки в `compose.yaml` поднимаются 6 нод Valkey: 3 master и 3 replica.

Rate limiting включается на уровне Scrapper API. По умолчанию лимит - 60 запросов в минуту на ключ, параметры задаются через `SCRAPPER_RATE_LIMIT_*`.

## Наблюдаемость

`bot` и `scrapper` отдают Prometheus-метрики на отдельных management-портах:

- `http://localhost:8011/metrics` для `bot`.
- `http://localhost:8012/metrics` для `scrapper`.

В проекте настроены:

- Pull-сбор метрик Prometheus.
- Опциональная Push-модель через Pushgateway.
- Grafana datasource provisioning.
- RED dashboard и business dashboard.
- Grafana alert на повышенное потребление RAM.
- PromQL-запросы в `example_pql.txt`.
- Подробное описание в `OBSERVABILITY.md`.

Основные пользовательские метрики:

- `links_on_track_total{tracked_source}`.
- `request_duration_ms_total{scope,scope_type}`.
- `api_requests_total{source}`.
- `command_requests_total{command}`.
- `command_duration_ms_total{scope,scope_type}`.
- `command_handling_duration_ms_total{command}`.
- `telegram_requests_total{request_type}`.
- `sent_notification_total`.

## Полезные Команды

Собрать проект:

```bash
mvn clean package
```

Запустить все тесты:

```bash
mvn test
```

Запустить тесты конкретного модуля:

```bash
mvn -pl scrapper test
mvn -pl bot test
mvn -pl ai-agent test
```

Запустить форматирование:

```bash
mvn spotless:apply
```

Запустить статические проверки:

```bash
mvn pmd:check spotbugs:check
```

Посмотреть логи контейнера:

```bash
docker compose logs scrapper
docker compose logs bot
```

Остановить окружение:

```bash
docker compose down
```

## Что Проверить, Если Проект Не Стартует

Для `bot`:

- задан ли `TELEGRAM_TOKEN`;
- доступен ли `scrapper` по `BOT_SCRAPPER_BASE_URL`;
- совпадают ли Kafka topic и bootstrap servers с настройками `scrapper`/`ai-agent`.

Для `scrapper`:

- доступна ли PostgreSQL;
- применились ли Flyway-миграции;
- корректны ли `GITHUB_TOKEN`, `STACKOVERFLOW_KEY`, `STACKOVERFLOW_ACCESS_KEY`;
- правильно ли задан `SCRAPPER_ACCESS_TYPE`;
- запущены ли Kafka и Schema Registry при `SCRAPPER_NOTIFICATION_TRANSPORT=KAFKA`;
- доступен ли Valkey cluster, если включен `SCRAPPER_VALKEY_CACHE_ENABLED=true`.

Для `ai-agent`:

- доступны ли Kafka brokers;
- существует ли topic `link.raw-updates`;
- задан ли `AI_AGENT_SUMMARIZATION_API_TOKEN`, если выбран API-провайдер суммаризации;
- совпадает ли выходной topic с `BOT_KAFKA_UPDATES_TOPIC`.

## Дополнительные Материалы

- `OBSERVABILITY.md` - подробности по метрикам, Grafana и PromQL.
- `scrapper/src/test/jmeter/link-list-load-testing.md` - описание нагрузочного тестирования кэша.
- `contract/src/main/resources/scrapper-api.yaml` и `contract/src/main/resources/bot-api.yaml` - API-контракты.
- `contract/src/main/resources/avro/LinkUpdateEvent.avsc` - Avro-схема Kafka-события.
