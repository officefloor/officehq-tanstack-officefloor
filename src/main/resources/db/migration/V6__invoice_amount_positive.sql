-- An invoice must be for something: its amount has to be MORE THAN ZERO. This adds the guard at the
-- data layer so a zero/negative amount can never be persisted, matching the UI (invoiceForm.slot.tsx)
-- and server (CreateInvoice.java) checks.
ALTER TABLE invoices
    ADD CONSTRAINT invoices_amount_positive
    CHECK (amount > 0);
