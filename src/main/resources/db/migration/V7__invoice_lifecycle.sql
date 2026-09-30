-- Invoices now move through a lifecycle: DRAFT -> SENT -> PAID. A new invoice starts as a DRAFT
-- (it has not gone out yet), the app records it as SENT when it is sent, and SENT when payment is
-- taken. Repoint the status default from the old 'UNPAID' to 'DRAFT' and migrate any existing
-- unpaid rows to the new starting stage.
ALTER TABLE invoices ALTER COLUMN status SET DEFAULT 'DRAFT';
UPDATE invoices SET status = 'DRAFT' WHERE status = 'UNPAID';
