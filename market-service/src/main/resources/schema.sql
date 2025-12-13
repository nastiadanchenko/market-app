-- создать orders
CREATE SEQUENCE IF NOT EXISTS orders_id_seq AS BIGINT;
CREATE TABLE if not exists public.orders
(
    id        bigint NOT NULL primary key default (nextval('orders_id_seq'::regclass)),
    total_sum int8   not null default 0
);

-- создать items
CREATE SEQUENCE IF NOT EXISTS items_id_seq AS BIGINT;
CREATE TABLE if not exists public.items
(
    id          bigint NOT NULL primary key default (nextval('items_id_seq'::regclass)),
    title       varchar(255)      NULL,
    description varchar(1000)     NULL,
    img_path    varchar(255)      NOT NULL,
    price       numeric DEFAULT 0.00 NOT NULL,
    count       int4    DEFAULT 0 NOT NULL
);

-- создать orders_items
CREATE SEQUENCE IF NOT EXISTS orders_items_id_seq AS BIGINT;

CREATE TABLE if not exists public.orders_items
(
    id       bigint NOT NULL primary key default (nextval('orders_items_id_seq'::regclass)),
    item_id  int8      NOT NULL,
    order_id int8      NOT NULL,
    count    int4      NULL,
    CONSTRAINT fk_orders_items_item FOREIGN KEY (item_id) REFERENCES public.items (id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_items_order FOREIGN KEY (order_id) REFERENCES public.orders (id) ON DELETE CASCADE
);



