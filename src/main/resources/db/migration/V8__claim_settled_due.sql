-- A settled claim's due, fixed when it was settled: its amount times its person's grade's rate at that
-- moment. Null for a claim not settled, or settled while the grade had no rate (its due stays live).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(15, 2);
