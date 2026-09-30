-- Sorting a project's invoices by due date orders on invoice.due_date. An index keeps that ordering
-- fast as the invoice list grows. Additive index, no data change; existing rows and the create/pay
-- paths are unaffected.
CREATE INDEX idx_invoice_due_date ON invoice (due_date);
