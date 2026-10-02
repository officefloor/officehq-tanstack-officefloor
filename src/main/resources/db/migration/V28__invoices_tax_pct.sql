-- An invoice can carry a percentage sales tax added ON TOP of its net total — the subtotal less any
-- discount (Flyway V27). Tax is worked out after the discount: (subtotal - discount) * tax_pct. A
-- new NUMERIC column holding the percentage (0-100), defaulting to 0 so every existing invoice has
-- no tax — its final amount equals its net total until a tax rate is set. The final amount is worked
-- out on the server (see InvoiceMoney), never typed.
ALTER TABLE invoices ADD COLUMN tax_pct NUMERIC(5, 2) NOT NULL DEFAULT 0;
