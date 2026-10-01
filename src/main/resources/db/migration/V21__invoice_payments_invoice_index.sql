-- An invoice's status is now DERIVED from its payments: on every read we sum the payments made
-- against the invoice (ListInvoices, GetInvoice) to work out PARTIAL / PAID. That lookup is always
-- by invoice_id, so index it — an additive index, no data change, matching the invoice_id read path
-- the derivation now leans on.
CREATE INDEX idx_invoice_payments_invoice ON invoice_payments (invoice_id);
