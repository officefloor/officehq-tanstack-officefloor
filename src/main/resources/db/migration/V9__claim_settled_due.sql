-- A settled claim's due, fixed when it is settled: its amount at its person's grade's rate then.
-- Null while the claim is not settled, or when the grade had no rate (its due then stays live).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(19, 2);
