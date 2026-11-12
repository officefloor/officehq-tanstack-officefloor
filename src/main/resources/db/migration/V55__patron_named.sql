-- The order a person's patrons were named in (their shares are split in it): taken when the patron is
-- added. Kept in patron_hist too; the history trigger is set again afterwards, as in V45__claim_tags.sql.
CREATE SEQUENCE patron_named_seq;
DROP TRIGGER patron_history;
ALTER TABLE patron ADD COLUMN named BIGINT DEFAULT NEXT VALUE FOR patron_named_seq NOT NULL;
ALTER TABLE patron_hist ADD COLUMN named BIGINT;
CREATE TRIGGER patron_history AFTER INSERT, UPDATE, DELETE ON patron FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
