-- An invoice must carry a positive amount: you cannot raise an invoice for nothing. Enforce it at
-- the schema level so a non-positive amount is rejected regardless of which path inserts the row,
-- mirroring the server-side check in InvoicesPost and the front-end form validation.
ALTER TABLE invoice ADD CONSTRAINT invoice_amount_positive CHECK (amount > 0);
