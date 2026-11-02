-- A closed or soft-closed period's carried figure per person, fixed with their due-total when it was (soft-)closed;
-- reopening the period drops it with the row.
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(15, 2) NOT NULL DEFAULT 0;
