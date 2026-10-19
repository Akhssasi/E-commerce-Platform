CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uk_category_name UNIQUE (name)
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT        NOT NULL REFERENCES categories(id),
    name        VARCHAR(200)  NOT NULL,
    sku         VARCHAR(32)   NOT NULL,
    price       NUMERIC(12,2) NOT NULL CHECK (price > 0),
    stock       INTEGER       NOT NULL CHECK (stock >= 0),
    active      BOOLEAN       NOT NULL DEFAULT true,
    image_url   VARCHAR(500),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_sku UNIQUE (sku)
);

CREATE INDEX idx_product_active_price ON products(active, price);
CREATE INDEX idx_product_category ON products(category_id);
