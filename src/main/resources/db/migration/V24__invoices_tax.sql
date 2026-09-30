-- An invoice can have a percentage SALES TAX added on top, worked out AFTER any discount: its final
-- total is (subtotal minus discount) times (1 plus the tax rate). Additive: a new NOT NULL column
-- with a default of 0, so any existing invoice keeps its former total (no tax) and remains valid. The
-- stored line-item amounts and the discount are untouched — the tax is applied on top when the
-- invoice's total is worked out.
ALTER TABLE invoices ADD COLUMN tax_pct DECIMAL(5, 2) NOT NULL DEFAULT 0;
