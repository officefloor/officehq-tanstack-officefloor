-- An invoice can have a percentage DISCOUNT taken off it: its final total is the subtotal (the sum of
-- its line items) minus that percentage. Additive: a new NOT NULL column with a default of 0, so any
-- existing invoice keeps its full amount (no discount) and remains valid. The stored line-item amounts
-- are untouched — the discount is applied on top when the invoice's total is worked out.
ALTER TABLE invoices ADD COLUMN discount_pct DECIMAL(5, 2) NOT NULL DEFAULT 0;
