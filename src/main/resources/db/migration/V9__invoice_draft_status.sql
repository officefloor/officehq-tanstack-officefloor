-- An invoice now moves through a lifecycle: DRAFT -> SENT -> PAID. A freshly raised invoice starts
-- as a DRAFT (it hasn't gone out yet), so the status column's default becomes 'DRAFT'. Additive: an
-- ALTER of the existing column default, no data rewrite — rows carry whatever status they were given.
ALTER TABLE invoices ALTER COLUMN status SET DEFAULT 'DRAFT';
