-- The people a claim is shared with and their share percents, in the order shared: <person id>:<0.00>, comma
-- separated (see net.officefloor.hq.app.claim.Shares); null when it is shared with nobody. Kept in claim_hist too;
-- the history trigger is set again afterwards, as in V84__claim_held.sql.
DROP TRIGGER claim_history;
ALTER TABLE claim ADD COLUMN shares VARCHAR(4000);
ALTER TABLE claim_hist ADD COLUMN shares VARCHAR(4000);
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
