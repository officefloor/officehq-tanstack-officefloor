-- Paying an invoice is a status transition, so an invoice now carries a status. New and existing
-- rows start UNPAID (the DEFAULT), and marking one paid flips it to PAID. A short VARCHAR holds the
-- status token the UI shows and the audit record names; NOT NULL + DEFAULT keeps every row valid.
ALTER TABLE invoices ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'UNPAID';
