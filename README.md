# LinkTracker Bot

LinkTracker — Telegram-бот, который отслеживает изменения на веб-страницах
и оперативно информирует пользователя о них.

## Инструкция по запуску

### 1. Получение токена

Если у вас ещё нет токена — создайте бота через [@BotFather](https://t.me/BotFather)
и скопируйте выданный токен. Он выглядит так: 1234512345:ABCDEFGHIJKLMNOP...

---

### 2. Настройка токена

Программа ожидает токен в переменной окружения `TELEGRAM_TOKEN`.

**Вариант А — через файл `application-local.yml`:**

Создайте файл `bot/src/main/resources/application-local.yml`

```yaml
app:
  telegram:
    token: "1234512345:ABCDEFG..."
```

**Вариант Б — через переменную окружения:**

macOS / Linux (bash/zsh):

```bash
export TELEGRAM_TOKEN=1234512345:ABCDEFG...
```

Windows CMD:

```bash
set TELEGRAM_TOKEN=1234512345:ABCDEFG...
```

**Вариант В — через Run Configuration в IntelliJ IDEA:**

Перейдите в `Run` → `Edit Configurations`.
В поле `Environment variables` добавьте:

```
TELEGRAM_TOKEN=1234512345:ABCDEFG...
```

### 3. Проект готов к сборке и запуску

