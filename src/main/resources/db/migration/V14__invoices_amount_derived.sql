-- The invoice amount is now DERIVED: it is the sum of the invoice's line items (Flyway V13), kept
-- in the amount column as a worked-out total rather than a figure typed by hand. An invoice with no
-- lines yet totals zero, so the "amount must be positive" rule (Flyway V6) no longer holds — a draft
-- can exist before anything has been charged to it. Drop that constraint; the total is still
-- recomputed on the server every time a line is added.
ALTER TABLE invoices DROP CONSTRAINT invoices_amount_positive;
