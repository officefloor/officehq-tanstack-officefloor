-- A settled claim's due, fixed when it is settled: its amount at its person's grade's rate at that
-- moment, so later rate changes do not alter it. Null while the claim is not settled, and for a
-- claim settled while its grade had no rate (its due stays live).
ALTER TABLE claim ADD COLUMN due DECIMAL(19, 2);
