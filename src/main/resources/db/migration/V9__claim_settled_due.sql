-- A settled claim's due, fixed when it was settled: its amount times its person's grade rate at that
-- moment, divided by 100, rounded half up to two decimals. NULL while a claim is not settled (its due
-- is live), and NULL for a settled claim whose grade had no rate then (its due stays live).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(14, 2);
UPDATE claim c SET settled_due = (
    SELECT ROUND(c.amount * r.pct / 100, 2) FROM person p JOIN rate r ON r.sheet_id = p.sheet_id AND r.grade = p.grade
    WHERE p.id = c.person_id)
WHERE c.status = 'SETTLED';
