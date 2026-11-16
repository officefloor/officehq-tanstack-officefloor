-- Closing or soft-closing a period also fixes each person's carried figure for it (see the periods screen), as it
-- fixes their due-total; reopening drops it with the row, so the figure is live again. Null: fixed before this was kept.
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(12, 2);

-- Each person's carried figure fixed by a close or soft close, as "personId=carried" joined by ";" (null on a reopen).
ALTER TABLE period_log ADD COLUMN person_carried VARCHAR(8000);
