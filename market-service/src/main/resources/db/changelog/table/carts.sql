CREATE SEQUENCE IF NOT EXISTS carts_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS carts
(
    id         BIGINT PRIMARY KEY DEFAULT nextval('carts_id_seq'),
    user_id    BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP          DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP          DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    total_price numeric DEFAULT 0.00
);