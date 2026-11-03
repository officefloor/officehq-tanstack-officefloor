-- A period's cap on open claims per person: how many OPEN or PENDING claims whose due-by falls in it a
-- person may have; a claim belonging to it is not opened beyond that. NULL for none.
ALTER TABLE period ADD COLUMN claim_cap INT;
