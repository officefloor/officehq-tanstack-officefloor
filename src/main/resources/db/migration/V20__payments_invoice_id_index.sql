-- How much is still left to pay on an invoice is now shown per row (amount minus its payments), so
-- the project invoice list sums each invoice's payments once per row. Index the column those sums
-- group by (invoice_id, matching the per-invoice payments query). Additive: no data changes.
CREATE INDEX idx_payments_invoice ON payments (invoice_id);
