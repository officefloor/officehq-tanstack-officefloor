-- Each person's carried figure for a closed or soft-closed period, fixed when it was closed alongside
-- their due-total for it (0.00 for one with nothing carried). Dropped with it when the period is reopened.
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(19, 2) DEFAULT 0 NOT NULL;
