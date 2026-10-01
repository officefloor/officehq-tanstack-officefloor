-- An invoice must bill a positive amount — you cannot raise an invoice for nothing. Enforce it at
-- the DB level too, matching the server + front-end validation: rejects zero and negative amounts,
-- so no bad row can slip in through any path.
ALTER TABLE invoices
    ADD CONSTRAINT invoices_amount_positive CHECK (amount > 0);
