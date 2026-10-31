-- A claim's due, fixed when it is settled: amount times its person's grade's rate then, divided
-- by 100, rounded half up to two decimals. Null while the claim is not settled, or when it was
-- settled with no rate (n/a): such a claim's due stays live, at the current rate.
ALTER TABLE claim ADD COLUMN due DECIMAL(14,2);
