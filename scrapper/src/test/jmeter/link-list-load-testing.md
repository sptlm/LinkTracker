# Нагрузочное тестирование списка ссылок

Нагрузочный тест настроен как обычный план Apache JMeter:

- `scrapper/src/test/jmeter/load-test.jmx`
- `scrapper/src/test/jmeter/seed_data.sql`

Для нагрузочного тестирования запускайте Scrapper с отключенным polling, чтобы сгенерированные
фейковые GitHub-ссылки не проверялись через реальный GitHub API:

```shell
$env:SCRAPPER_POLLING_ENABLED="false"
```

Seed-скрипт готовит набор данных под условия задания: `1000` чатов по `100` ссылок в каждом,
то есть всего `100000` отслеживаемых ссылок. План JMeter отправляет `GET /links` и `POST /links`
с примерным соотношением чтения и записи `99% / 1%`. POST-запросы используют уникальные ссылки
для мутаций, поэтому повторные запуски не падают из-за дублей.

## Подготовка данных

Сначала запустите инфраструктуру и Scrapper, затем примените seed-скрипт к базе Scrapper:

```shell
psql "postgresql://postgres:postgres@localhost:5433/linktracker" `
  -f scrapper/src/test/jmeter/seed_data.sql
```

Также очищайте Valkey перед каждым сравнительным прогоном, иначе один сценарий может использовать
cache-записи, оставшиеся от предыдущего:

```bash
valkey-cli -c -p 6380 FLUSHALL
```

## Запуск через JMeter GUI

Откройте `scrapper/src/test/jmeter/load-test.jmx` в графическом интерфейсе JMeter.

Полезные параметры уже вынесены в properties, их можно переопределять в GUI или из CLI:

- `scrapper.host`, значение по умолчанию `127.0.0.1`
- `scrapper.port`, значение по умолчанию `8081`
- `load.threads`, значение по умолчанию `16`
- `load.rampUp`, значение по умолчанию `60`
- `load.duration`, значение по умолчанию `300`
- `load.chatCount`, значение по умолчанию `1000`

## Запуск без GUI

Перед новым запуском удалите предыдущую директорию HTML-отчета: JMeter требует, чтобы выходная
директория отсутствовала или была пустой.

Перед стартом JMeter проверьте, что Scrapper действительно слушает тот же host и port:

```powershell
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8081/actuator/health
```

JMeter properties с `-J` нужно писать без пробела после `-J`; иначе JMeter некорректно прочитает
имя свойства и молча откатится к значениям по умолчанию.

PowerShell:

```shell
New-Item -ItemType Directory -Force scrapper/target/jmeter | Out-Null
Remove-Item -Recurse -Force scrapper/target/jmeter/html-report -ErrorAction SilentlyContinue

jmeter.bat -n `
  -t scrapper/src/test/jmeter/load-test.jmx `
  -Jscrapper.host=127.0.0.1 `
  -Jscrapper.port=8081 `
  -Jload.threads=16 `
  -Jload.rampUp=60 `
  -Jload.duration=300 `
  -Jload.chatCount=1000 `
  -l scrapper/target/jmeter/link-list-results.jtl `
  -e `
  -o scrapper/target/jmeter/html-report
```

Для сравнительных прогонов перезапускайте Scrapper с нужными cache-флагами и заново применяйте
`seed_data.sql` перед каждым сценарием. Polling должен быть отключен на протяжении всего
нагрузочного теста:

- общее для всех сценариев: `SCRAPPER_POLLING_ENABLED=false`
- без кэша: `SCRAPPER_VALKEY_CACHE_ENABLED=false`
- только Valkey cache: `SCRAPPER_VALKEY_CACHE_ENABLED=true`, `SCRAPPER_VALKEY_CLIENT_SIDE_CACHE_ENABLED=false`
- Valkey cache с client-side caching: `SCRAPPER_VALKEY_CACHE_ENABLED=true`, `SCRAPPER_VALKEY_CLIENT_SIDE_CACHE_ENABLED=true`

Если нода Valkey или topology кластера недоступны, Scrapper считает операции с кэшем best-effort
и продолжает работу без падения после `SCRAPPER_VALKEY_CACHE_OPERATION_TIMEOUT` (`500ms` по
умолчанию). Такие предупреждения означают, что замер уже не является корректным cache-бенчмарком:
перед повторным прогоном нужно перезапустить или пересоздать Valkey cluster.

Сгенерированный HTML-отчет JMeter содержит throughput, среднюю latency, p50/p99 перцентили,
количество ответов по HTTP-кодам и ошибки для `GET /links` и `POST /links`.
