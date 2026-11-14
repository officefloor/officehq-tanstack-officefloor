-- A period's cap on the open claims per person: opening a claim due by a date in it is refused while its person
-- already has that many OPEN or PENDING claims due by a date in it; null: no cap.
ALTER TABLE period ADD COLUMN claim_cap INT;
