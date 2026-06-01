# Наблюдаемость

Проект отдает Prometheus-метрики из сервисов `scrapper` и `bot`, собирает их через Prometheus и автоматически настраивает дашборды Grafana.

По умолчанию используется Pull-модель: Prometheus читает `/metrics` напрямую у приложений. Pushgateway остается в `compose.yaml` для дополнительного сценария, но отправка из приложений выключена переменными `SCRAPPER_PUSHGATEWAY_ENABLED=false` и `BOT_PUSHGATEWAY_ENABLED=false`, чтобы одни и те же метрики не попадали в Prometheus дважды.

## Запуск

```powershell
docker compose up -d --build
```

Сервисы:

- API Scrapper: `http://localhost:8081`
- Метрики Scrapper: `http://localhost:8012/metrics`
- API Bot: `http://localhost:8080`
- Метрики Bot: `http://localhost:8011/metrics`
- Prometheus: `http://localhost:9090`
- Pushgateway: `http://localhost:9091`
- Grafana: `http://localhost:3000` (`admin` / `admin` по умолчанию)

Секреты читаются из переменных окружения: `TELEGRAM_TOKEN`, `GITHUB_TOKEN`, `STACKOVERFLOW_KEY`, `STACKOVERFLOW_ACCESS_KEY`.

Для проверки Pushgateway можно явно включить отправку метрик:

```powershell
$env:SCRAPPER_PUSHGATEWAY_ENABLED = "true"
$env:BOT_PUSHGATEWAY_ENABLED = "true"
docker compose up -d --build
```

При одновременном Pull и Push нужно фильтровать источник метрик в PromQL, иначе counters и gauges будут завышены.

## Метрики

Scrapper:

- `links_on_track_total{tracked_source}` - gauge с количеством активных ссылок, поставленных на мониторинг в БД. Значение хранится в памяти приложения и обновляется при добавлении/удалении ссылки, поэтому `/metrics` не выполняет SQL-запрос при каждом scrape.
- `request_duration_ms_total{scope,scope_type}` - histogram длительности операций Scrapper в миллисекундах.
- `api_requests_total{source}` - counter входящих запросов к Scrapper API.

Значения `scope` для `request_duration_ms_total`:

- `database` - запросы к БД; `scope_type` соответствует операции в формате `repository#method`, например `link#findByUrl`.
- `scrape` - одна логическая операция проверки ссылки; `scope_type` равен источнику `github` или `stackoverflow`.
- `external_source` - отдельные HTTP-запросы к внешним источникам; `scope_type` равен домену источника.
- `kafka` - завершение отправки сообщения в Kafka broker; `scope_type` равен имени topic.
- `bot_api` - HTTP-вызов Scrapper в Bot API; `scope_type` равен методу API.

Bot:

- `command_requests_total{command}` - counter обработанных команд бота. Неизвестные команды нормализуются в `command="unknown"`.
- `command_duration_ms_total{scope,scope_type}` - histogram вызовов из Bot в Scrapper API.
- `command_handling_duration_ms_total{command}` - histogram полной длительности обработки команды.
- `telegram_requests_total{request_type}` - counter входящих событий из Telegram. Значения `message`, `dialog`, `command`, `blank_message`, `unsupported` взаимоисключающие.
- `sent_notification_total` - counter успешно отправленных нотификаций. Счетчик увеличивается только после успешного ответа Telegram API (`SendResponse.isOk()`).

Также отдаются стандартные метрики Spring Boot/Micrometer, включая `http_server_requests_seconds_*` для RED-метрик, `process_resident_memory_bytes` для RAM процесса и `jvm_memory_used_bytes` для JVM memory pools.

## Дашборды

Дашборды Grafana автоматически подхватываются из файлов:

- `grafana/dashboards/red-dashboard.json`
- `grafana/dashboards/business-dashboard.json`

RED-дашборд содержит переменную `$application`. Бизнес-дашборд содержит переменные `$application` и `$app_type`, где `$app_type` может принимать значения `bot` или `scrapper`.

## Алерт

Grafana настраивает алерт из файла `grafana/provisioning/alerting/ram-alert.yml`. Алерт срабатывает, если RSS/RAM процесса приложения держится выше 512 MiB в течение 2 минут:

```promql
sum by (application) (process_resident_memory_bytes)
```

В репозитории не задан contact point для внешней доставки алертов, потому что для Telegram или другого канала нужны секреты. В рабочем окружении contact point и notification policy нужно добавить в Grafana через переменные окружения/секреты или UI.

## PromQL

Набор запросов также лежит в `example_pql.txt`. Ниже перечислены запросы, на которых построены панели.

|          Панель           |                           Назначение                            |                                                                               PromQL                                                                               |
|---------------------------|-----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| HTTP request rate         | RPS по приложениям и статусам                                   | `sum by (application, status) (rate(http_server_requests_seconds_count{application=~"$application"}[1m]))`                                                         |
| HTTP 5xx error rate       | Частота HTTP 5xx                                                | `sum by (application) (rate(http_server_requests_seconds_count{application=~"$application",status=~"5.."}[1m]))`                                                   |
| HTTP latency p50          | p50 latency HTTP                                                | `histogram_quantile(0.50, sum by (le, application) (rate(http_server_requests_seconds_bucket{application=~"$application"}[5m])))`                                  |
| HTTP latency p95          | p95 latency HTTP                                                | `histogram_quantile(0.95, sum by (le, application) (rate(http_server_requests_seconds_bucket{application=~"$application"}[5m])))`                                  |
| HTTP latency p99          | p99 latency HTTP                                                | `histogram_quantile(0.99, sum by (le, application) (rate(http_server_requests_seconds_bucket{application=~"$application"}[5m])))`                                  |
| Process RAM               | RSS/RAM процесса приложения                                     | `sum by (application) (process_resident_memory_bytes{application=~"$application"})`                                                                                |
| JVM memory pools          | Heap/non-heap pools JVM                                         | `sum by (application, area, id) (jvm_memory_used_bytes{application=~"$application"})`                                                                              |
| User messages per second  | Скорость входящих пользовательских сообщений, диалогов и команд | `sum by (request_type) (rate(telegram_requests_total{application=~"$application",app_type=~"$app_type",request_type=~"message|dialog|command"}[1m]))`              |
| Active tracked links      | Количество активных ссылок по источнику                         | `sum by (tracked_source) (links_on_track_total{application=~"$application",app_type=~"$app_type"})`                                                                |
| Scrape p50                | p50 логической scrape-операции по источнику                     | `histogram_quantile(0.50, sum by (le, scope_type) (rate(request_duration_ms_total_bucket{application=~"$application",app_type=~"$app_type",scope="scrape"}[5m])))` |
| Scrape p95                | p95 логической scrape-операции по источнику                     | `histogram_quantile(0.95, sum by (le, scope_type) (rate(request_duration_ms_total_bucket{application=~"$application",app_type=~"$app_type",scope="scrape"}[5m])))` |
| Scrape p99                | p99 логической scrape-операции по источнику                     | `histogram_quantile(0.99, sum by (le, scope_type) (rate(request_duration_ms_total_bucket{application=~"$application",app_type=~"$app_type",scope="scrape"}[5m])))` |
| Bot command p50           | p50 полной обработки команды Bot                                | `histogram_quantile(0.50, sum by (le, command) (rate(command_handling_duration_ms_total_bucket{application=~"$application",app_type=~"$app_type"}[5m])))`          |
| Bot command p95           | p95 полной обработки команды Bot                                | `histogram_quantile(0.95, sum by (le, command) (rate(command_handling_duration_ms_total_bucket{application=~"$application",app_type=~"$app_type"}[5m])))`          |
| Bot command p99           | p99 полной обработки команды Bot                                | `histogram_quantile(0.99, sum by (le, command) (rate(command_handling_duration_ms_total_bucket{application=~"$application",app_type=~"$app_type"}[5m])))`          |
| Telegram bot requests     | Скорость запросов к боту по типам                               | `sum by (request_type) (rate(telegram_requests_total{application=~"$application",app_type=~"$app_type"}[1m]))`                                                     |
| Sent notifications        | Скорость успешно отправленных уведомлений                       | `sum(rate(sent_notification_total{application=~"$application",app_type=~"$app_type"}[1m]))`                                                                        |
| Scrapper API requests     | Скорость запросов к Scrapper API по источнику                   | `sum by (source) (rate(api_requests_total{application=~"$application",app_type=~"$app_type"}[1m]))`                                                                |
| Bot calls to Scrapper API | p95 вызовов Bot в Scrapper API                                  | `histogram_quantile(0.95, sum by (le, scope_type) (rate(command_duration_ms_total_bucket{application=~"$application",scope="scrapper_sync_api"}[5m])))`            |

## Сборка и публикация Docker-образов

Локальная сборка выполняется через `docker compose up -d --build`. Для публикации в registry используйте тот же контекст сборки и явные теги:

```powershell
$env:REGISTRY = "registry.example.com/link-tracker"
$env:IMAGE_TAG = "0.0.1"

docker login $env:REGISTRY

docker build -f scrapper/Dockerfile -t "${env:REGISTRY}/scrapper:${env:IMAGE_TAG}" .
docker build -f bot/Dockerfile -t "${env:REGISTRY}/bot:${env:IMAGE_TAG}" .

docker push "${env:REGISTRY}/scrapper:${env:IMAGE_TAG}"
docker push "${env:REGISTRY}/bot:${env:IMAGE_TAG}"
```

Для GitLab CI эти команды можно перенести в job с переменными `CI_REGISTRY_IMAGE` и `CI_COMMIT_TAG`/`CI_COMMIT_SHORT_SHA`.

