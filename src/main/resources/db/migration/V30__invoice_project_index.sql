-- Sorting clients by how much they owe joins invoices back to their client through the invoice's
-- project. An index on invoice.project_id keeps that per-client outstanding aggregation (and the
-- existing per-project sums) fast as the invoice list grows. Additive index, no data change;
-- existing rows and the create path are unaffected.
CREATE INDEX idx_invoice_project ON invoice (project_id);
