CREATE SEQUENCE IF NOT EXISTS carts_items_id_seq START WITH 1 INCREMENT BY 1;

-- Создание таблицы carts_items (товары в корзине)
CREATE TABLE IF NOT EXISTS carts_items
(
    id         BIGINT PRIMARY KEY DEFAULT nextval('carts_items_id_seq'),
    cart_id    BIGINT  NOT NULL,
    item_id    BIGINT  NOT NULL,
    count INTEGER NOT NULL   DEFAULT 1 CHECK (count > 0),
    added_at   TIMESTAMP          DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cart_id, item_id),                                    -- Один товар может быть в корзине только один раз
    FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE -- предполагается, что таблица items существует
);