-- The people a claim is shared with, in the order added: <person id>:<share> pairs, comma separated (each share two
-- decimals, more than 0.00, together at most 100.00); null for a claim shared with no one. Each holder's due-total
-- counts their part of the claim's due, and the claim's own person's the rest.
ALTER TABLE claim ADD COLUMN shares VARCHAR(2000);
