-- An invoice can carry a percentage discount taken off its subtotal (the worked-out sum of its line
-- items). The final total billed is the subtotal less that percentage. A new NUMERIC column holding
-- the percentage (0-100), defaulting to 0 so every existing invoice has no discount — its final
-- total equals its subtotal until a discount is set.
ALTER TABLE invoices ADD COLUMN discount_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;
