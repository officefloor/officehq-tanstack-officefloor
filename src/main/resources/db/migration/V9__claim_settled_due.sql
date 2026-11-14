-- A settled claim's due, fixed when it is settled: its amount at its person's grade's rate then.
-- None while not settled, or when the grade had no rate (then the due stays live).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(19, 2);
