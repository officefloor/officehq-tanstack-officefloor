-- A settled claim's due, fixed when it was settled: its amount times its person's grade's rate then,
-- divided by 100, rounded half up. Null while the claim is not settled, or when the grade had no rate
-- then (its due stays live).
ALTER TABLE claim ADD COLUMN due DECIMAL(19, 2);
