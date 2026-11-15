-- A closed or soft-closed period's carried figure per person: the due of their claims due by a day in
-- an earlier period still OPEN or PENDING at the end of its last day, fixed when it was (soft-)closed
-- with their due-total. Removed with it when the period is reopened.
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(19, 2) DEFAULT 0 NOT NULL;
