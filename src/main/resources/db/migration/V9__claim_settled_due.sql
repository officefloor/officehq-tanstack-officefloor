-- A claim's due fixed when it is settled: its amount times its person's grade's rate at that
-- moment, rounded half up to two decimals. Null while it is not settled, or when the grade had no
-- rate then (its due stays live).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(14, 2);
