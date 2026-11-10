-- A settled claim's due, fixed when it was settled: its amount times its person's grade's rate at
-- that moment. Null for a claim not settled, or settled while its grade had no rate (its due stays
-- live, as an open claim's does).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(19, 2);
