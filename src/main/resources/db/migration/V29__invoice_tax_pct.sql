-- An invoice can carry a sales-tax percentage added on top AFTER any discount: the final amount is
-- (subtotal minus the discount) times (1 plus this tax rate). Additive column defaulting to 0, so
-- existing invoices and any caller that omits it have no tax. The subtotal, the discount amount and
-- the discounted total are unchanged; the tax amount and the taxed final amount are DERIVED from this
-- percentage, not stored. NUMERIC(5, 2) allows a fractional percent (e.g. 8.25) while a plain 20
-- reads as twenty percent.
ALTER TABLE invoice ADD COLUMN tax_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;
