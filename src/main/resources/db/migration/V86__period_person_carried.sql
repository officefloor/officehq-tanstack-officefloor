-- Each person's carried figure for a closed or soft-closed period, fixed with their due-total when it was closed:
-- the dues of their claims from earlier periods still open at its end. Cleared with the row when the period reopens.
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(19, 2) NOT NULL DEFAULT 0;
