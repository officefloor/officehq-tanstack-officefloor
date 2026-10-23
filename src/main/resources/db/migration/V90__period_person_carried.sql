-- Each person's carried figure in a closed or soft-closed period, fixed with their due-total when it was closed
-- (NULL: closed before carried figures were fixed, so it is shown live). Dropped with the totals on a reopen.
ALTER TABLE period_person_total ADD COLUMN carried DECIMAL(14, 2);
ALTER TABLE period_history_total ADD COLUMN carried DECIMAL(14, 2);
