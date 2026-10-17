-- A settled claim's due, fixed when it is settled: amount × its person's grade rate / 100, rounded half
-- up to two decimals. Later rate changes do not alter it. None while DRAFT or OPEN (their due is live),
-- nor when the grade had no rate at settling. Claims already settled take it from today's rates.
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(14, 2);
UPDATE claim c SET settled_due = (
    SELECT ROUND(c.amount * r.rate / 100, 2)
    FROM person p JOIN rate r ON r.sheet_id = p.sheet_id AND r.grade = p.grade
    WHERE p.id = c.person_id)
WHERE c.status = 'SETTLED';
