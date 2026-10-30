-- A policy version keeps the value it saved, so the versions can be shown as they were made. A version made before
-- values were kept takes the value its setting holds from its from-date (from the beginning of time when it has none).
-- Kept in history too (see V101); the journal trigger is made again around the change, as in V95.
DROP TRIGGER policy_version_journal;
ALTER TABLE policy_version ADD COLUMN setting_value VARCHAR(255);
ALTER TABLE policy_version_history ADD COLUMN setting_value VARCHAR(255);
UPDATE policy_version v SET setting_value = (SELECT f.setting_value FROM setting_from f WHERE f.sheet_id = v.sheet_id
    AND f.setting_key = v.setting_key AND f.from_date = COALESCE(v.from_date, DATE '0001-01-01'));
UPDATE policy_version_history v SET setting_value = (SELECT p.setting_value FROM policy_version p WHERE p.sheet_id = v.sheet_id
    AND p.version_no = v.version_no);
CREATE TRIGGER policy_version_journal AFTER INSERT, UPDATE, DELETE ON policy_version FOR EACH ROW CALL 'net.officefloor.hq.app.history.Journal';
