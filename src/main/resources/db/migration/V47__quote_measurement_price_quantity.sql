ALTER TABLE quote_item_measurements
    ADD COLUMN quantity NUMERIC(10,2),
    ADD COLUMN unit_price NUMERIC(12,2);

UPDATE quote_item_measurements AS measurement
SET quantity = COALESCE(item.quantity, 1),
    unit_price = COALESCE(item.unit_price, 0)
FROM quote_items AS item
WHERE item.id = measurement.quote_item_id;
