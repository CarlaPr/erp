-- O formulário permite combinar várias formas de pagamento.
-- Alinha a coluna ao limite já declarado na entidade Quote, sem truncar dados.
ALTER TABLE quotes ALTER COLUMN payment_method TYPE VARCHAR(255);
