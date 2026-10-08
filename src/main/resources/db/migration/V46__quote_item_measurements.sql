CREATE TABLE quote_item_measurements (
    quote_item_id UUID NOT NULL REFERENCES quote_items(id) ON DELETE CASCADE,
    measurement_order INTEGER NOT NULL,
    width NUMERIC(10,2) NOT NULL CHECK (width > 0),
    height NUMERIC(10,2) NOT NULL CHECK (height > 0),
    PRIMARY KEY (quote_item_id, measurement_order)
);
