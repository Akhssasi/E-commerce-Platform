CREATE TABLE carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_cart_user UNIQUE (user_id)
);

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT  NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id BIGINT  NOT NULL REFERENCES products(id),
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT uk_cart_item_product UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    order_no          VARCHAR(32)   NOT NULL,
    user_id           BIGINT        NOT NULL REFERENCES users(id),
    status            VARCHAR(32)   NOT NULL,
    total             NUMERIC(14,2) NOT NULL DEFAULT 0,
    currency          VARCHAR(3)    NOT NULL DEFAULT 'USD',
    payment_reference VARCHAR(120),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    paid_at           TIMESTAMPTZ,
    CONSTRAINT uk_order_no UNIQUE (order_no)
);

CREATE INDEX idx_order_user ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);

CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    sku          VARCHAR(32)   NOT NULL,
    product_name VARCHAR(200)  NOT NULL,
    unit_price   NUMERIC(12,2) NOT NULL,
    quantity     INTEGER       NOT NULL CHECK (quantity > 0),
    line_total   NUMERIC(14,2) NOT NULL
);

CREATE INDEX idx_order_item_order ON order_items(order_id);

CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES orders(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
