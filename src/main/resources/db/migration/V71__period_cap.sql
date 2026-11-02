-- The most OPEN or PENDING claims a person may have in the period (those whose due-by falls in it); null for no cap.
ALTER TABLE period ADD COLUMN claim_cap INT;
