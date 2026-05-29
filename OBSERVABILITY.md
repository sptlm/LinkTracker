# Наблюдаемость

Проект отдает Prometheus-метрики из сервисов `scrapper` и `bot`, собирает их через Prometheus, автоматически настраивает дашборды Grafana и, при необходимости, отправляет метрики приложений в Prometheus Pushgateway.

## Запуск

```powershell
docker compose up -d --build
```

Сервисы:

- API Scrapper: `http://localhost:8081`
- Метрики Scrapper: `http://localhost:8081/metrics`
- API Bot: `http://localhost:8080`
- Метрики Bot: `http://localhost:8011/metrics`
- Prometheus: `http://localhost:9090`
- Pushgateway: `http://localhost:9091`
- Grafana: `http://localhost:3000` (`admin` / `admin` по умолчанию)

Секреты читаются из переменных окружения: `TELEGRAM_TOKEN`, `GITHUB_TOKEN`, `STACKOVERFLOW_KEY`, `STACKOVERFLOW_ACCESS_KEY`.

## Метрики

Scrapper:

- `links_on_track_total{tracked_source}` - gauge с количеством активных ссылок, поставленных на мониторинг в БД.
- `request_duration_ms_total{scope,scope_type}` - histogram длительности операций с БД, Kafka, Bot API и внешними источниками.
- `api_requests_total{source}` - counter входящих запросов к Scrapper API.

Bot:

- `command_requests_total{command}` - counter обработанных команд бота.
- `command_duration_ms_total{scope,scope_type}` - histogram вызовов из Bot в Scrapper API.
- `command_handling_duration_ms_total{command}` - histogram полной длительности обработки команды.
- `telegram_requests_total{request_type}` - counter входящих событий из Telegram.
- `sent_notification_total` - counter успешно отправленных нотификаций.

Также отдаются стандартные метрики Spring Boot/Micrometer, включая `http_server_requests_seconds_*` для RED-метрик и `jvm_memory_used_bytes` для потребления памяти.

## Дашборды

Дашборды Grafana автоматически подхватываются из файлов:

- `grafana/dashboards/red-dashboard.json`
- `grafana/dashboards/business-dashboard.json`

RED-дашборд содержит переменную `$application`. Бизнес-дашборд содержит переменные `$application` и `$app_type`, где `$app_type` может принимать значения `bot` или `scrapper`.

## Алерт

Grafana настраивает алерт по RAM из файла `grafana/provisioning/alerting/ram-alert.yml`. Алерт срабатывает, если использование JVM heap держится выше 80% в течение 2 минут:

```promql
sum by (application) (jvm_memory_used_bytes{area="heap"})
/
clamp_min(sum by (application) (jvm_memory_max_bytes{area="heap"}), 1)
* 100
```

## PromQL

Полный список запросов, на которых построены визуализации дашбордов, находится в `example_pql.txt`.
