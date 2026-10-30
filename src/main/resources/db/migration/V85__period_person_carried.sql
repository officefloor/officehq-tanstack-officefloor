-- A close or soft-close fixes each person's carried figure for the period too, as it fixes their due-total.
-- NULL: fixed before carried figures were kept (the carried figure is then worked live).
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(14,2);
-- The carried figures a close or soft-close fixed, as person_totals ("<person id>=<carried>" joined by ";").
ALTER TABLE period_history ADD COLUMN person_carried VARCHAR(20000);
