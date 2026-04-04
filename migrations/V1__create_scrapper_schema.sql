CREATE TABLE tg_chat (
                         chat_id BIGINT PRIMARY KEY,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tracked_link (
                              id BIGSERIAL PRIMARY KEY,
                              url TEXT NOT NULL UNIQUE,
                              type TEXT NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              last_checked_at TIMESTAMPTZ,
                              last_updated_at TIMESTAMPTZ
);

CREATE INDEX idx_tracked_link_url ON tracked_link (url);
CREATE INDEX idx_tracked_link_last_checked_at ON tracked_link (last_checked_at);

CREATE TABLE link_subscription (
                                   id BIGSERIAL PRIMARY KEY,
                                   chat_id BIGINT NOT NULL,
                                   link_id BIGINT NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   CONSTRAINT uq_link_subscription_chat_link UNIQUE (chat_id, link_id),
                                   CONSTRAINT fk_link_subscription_chat
                                       FOREIGN KEY (chat_id) REFERENCES tg_chat (chat_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_link_subscription_link
                                       FOREIGN KEY (link_id) REFERENCES tracked_link (id) ON DELETE CASCADE
);

CREATE INDEX idx_link_subscription_chat_id ON link_subscription (chat_id);
CREATE INDEX idx_link_subscription_link_id ON link_subscription (link_id);

CREATE TABLE subscription_tag (
                                  subscription_id BIGINT NOT NULL,
                                  tag TEXT NOT NULL,
                                  PRIMARY KEY (subscription_id, tag),
                                  CONSTRAINT fk_subscription_tag_subscription
                                      FOREIGN KEY (subscription_id)
                                          REFERENCES link_subscription (id)
                                          ON DELETE CASCADE
);

CREATE INDEX idx_subscription_tag_tag ON subscription_tag (tag);
