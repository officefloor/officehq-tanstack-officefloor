-- The user who last edited a claim (through the claim form or the row's amount entry); null when it was
-- never edited. Like its opener, that user may not settle or approve it. Kept in claim_hist too; the
-- history trigger is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(255);
ALTER TABLE claim_hist ADD COLUMN edited_by VARCHAR(255);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
