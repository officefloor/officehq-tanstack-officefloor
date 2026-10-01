-- An invoice can carry a percentage sales TAX, applied on top AFTER any discount: the subtotal is
-- the sum of its line items, the discount is a percentage taken off that, and the tax is a
-- percentage of what is left (subtotal minus discount). The final total is the discounted amount
-- plus that tax. It is a percentage, so a fixed-scale DECIMAL between 0 and 100. NOT NULL
-- defaulting to 0 — an invoice without a tax set simply adds nothing on, and existing invoices get
-- 0 so their total is unchanged.
ALTER TABLE invoices ADD COLUMN tax_pct DECIMAL(5, 2) NOT NULL DEFAULT 0
    CHECK (tax_pct >= 0 AND tax_pct <= 100);
