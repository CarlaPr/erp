
ALTER TABLE quotes ADD COLUMN delivery_business_days INTEGER;
ALTER TABLE quotes ADD COLUMN delivery_date DATE;
ALTER TABLE quotes ADD CONSTRAINT quotes_delivery_business_days_positive
    CHECK (delivery_business_days IS NULL OR delivery_business_days > 0);
