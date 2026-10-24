-- A period's cap on the number of open claims per person: opening a claim whose due-by falls in it is refused when
-- its person already has that many OPEN or PENDING claims in it. Null for none.
ALTER TABLE period ADD COLUMN open_cap INTEGER;
