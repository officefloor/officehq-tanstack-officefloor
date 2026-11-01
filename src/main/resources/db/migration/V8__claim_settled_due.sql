-- A claim's due fixed when it is settled: its amount times its person's grade rate at that moment,
-- in hundredths rounded half up. Null while not settled, and when the grade had no rate.
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(15, 2);
