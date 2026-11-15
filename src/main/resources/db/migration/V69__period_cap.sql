-- The most OPEN or PENDING claims a person may have in the period (claims due by a day in it); null: no cap.
ALTER TABLE period ADD COLUMN claim_cap INT;
