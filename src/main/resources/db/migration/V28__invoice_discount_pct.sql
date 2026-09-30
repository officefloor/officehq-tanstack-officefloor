-- An invoice can carry a percentage discount taken off its subtotal (the sum of its line items),
-- giving a final total of subtotal minus that discount. Additive column defaulting to 0, so existing
-- invoices and any caller that omits it have no discount. The line amounts and the subtotal are
-- unchanged; the discount amount and the final total are DERIVED from this percentage, not stored.
-- NUMERIC(5, 2) allows a fractional percent (e.g. 12.50) while a plain 10 reads as ten percent.
ALTER TABLE invoice ADD COLUMN discount_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;
