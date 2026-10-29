-- A period may cap how many OPEN or PENDING claims each person has in it (null: no cap). Opening a claim that belongs
-- to the period (its due-by falls in it) is refused once its person has that many. Kept in the period's history too
-- (see V40). The journal trigger is made again around the change, as in V65.
DROP TRIGGER period_journal;
ALTER TABLE period ADD COLUMN claim_cap INT;
ALTER TABLE period_history ADD COLUMN claim_cap INT;
CREATE TRIGGER period_journal AFTER INSERT, UPDATE, DELETE ON period FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
