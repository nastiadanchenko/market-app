ALTER TABLE orders
    ADD COLUMN user_id BIGINT NOT NULL default 1,
    ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;


ALTER TABLE orders_items
    ADD COLUMN user_id BIGINT NOT NULL default 1,
    ADD CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
