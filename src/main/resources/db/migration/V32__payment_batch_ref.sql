-- A lump payment can be split across several invoices at once: the client hands over one sum and it
-- is allocated across their open invoices — one payment row per invoice (amount = that invoice's
-- share), so each invoice's balance reflects its share. This nullable column records, on each such
-- allocation, the reference of the lump payment it came from, so the rows that made up one payment
-- can be traced back together. Additive and nullable: a single-invoice payment (POST /api/payments)
-- leaves it null, so existing rows and the existing pay path need no change.
ALTER TABLE payment ADD COLUMN batch_ref VARCHAR(64);
