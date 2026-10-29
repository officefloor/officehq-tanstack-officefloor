-- A rate saved with a from-date on or after the sheet's today is forward-only: it applies only to claims added after
-- it was saved. claims_after is the highest claim id there was (in any sheet) when it was saved: it applies only to
-- claims with a higher id. Null (a rate with no from-date, or one before the sheet's today when saved) applies to every
-- claim. Kept in the rate's history and versions too. The journal trigger is made again around the change, as in V71.
DROP TRIGGER rate_journal;
ALTER TABLE rate ADD COLUMN claims_after BIGINT;
ALTER TABLE rate_history ADD COLUMN claims_after BIGINT;
ALTER TABLE rate_version ADD COLUMN claims_after BIGINT;
CREATE TRIGGER rate_journal AFTER INSERT, UPDATE, DELETE ON rate FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
