ALTER TABLE quotes ADD COLUMN profile_id UUID;
ALTER TABLE quotes ADD CONSTRAINT fk_quote_profile FOREIGN KEY (profile_id) REFERENCES profiles(id);