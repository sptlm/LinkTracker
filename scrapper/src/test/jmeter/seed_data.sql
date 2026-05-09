TRUNCATE TABLE notification_outbox, subscription_tag, link_subscription, tracked_link, tg_chat
    RESTART IDENTITY CASCADE;

INSERT INTO tg_chat(chat_id)
SELECT generate_series(1, 1000);

INSERT INTO tracked_link(id, url, type, created_at)
SELECT
    id,
    'https://github.com/loadtest/preseed-' || id,
    'GITHUB',
    now()
FROM generate_series(1, 100000) AS id;

INSERT INTO link_subscription(id, chat_id, link_id, created_at)
SELECT
    id,
    ((id - 1) / 100) + 1,
    id,
    now()
FROM generate_series(1, 100000) AS id;

SELECT setval('tracked_link_id_seq', 100000, true);
SELECT setval('link_subscription_id_seq', 100000, true);
