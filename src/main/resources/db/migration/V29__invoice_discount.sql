-- An invoice can carry a percentage DISCOUNT: the user takes a percentage off the subtotal (the sum
-- of its line items), and the final total is the subtotal minus that discount. It is a percentage,
-- so a fixed-scale DECIMAL between 0 and 100. NOT NULL defaulting to 0 — an invoice without a
-- discount set simply takes nothing off, and existing invoices get 0 so their total equals their
-- subtotal as before.
ALTER TABLE invoices ADD COLUMN discount_pct DECIMAL(5, 2) NOT NULL DEFAULT 0
    CHECK (discount_pct >= 0 AND discount_pct <= 100);
