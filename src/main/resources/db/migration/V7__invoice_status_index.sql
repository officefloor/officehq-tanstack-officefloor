-- The dashboard's outstanding total sums invoice amounts filtered by status ('UNPAID'). An index on
-- invoice.status keeps that aggregate fast as the invoice list grows. Additive index, no data
-- change; existing rows and the create/pay paths are unaffected.
CREATE INDEX idx_invoice_status ON invoice (status);
