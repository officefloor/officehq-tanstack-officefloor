-- The sheet's claim templates: a name (trimmed, at most once per sheet), the amount and the note a claim
-- made from one starts with. Saving a name again replaces its amount and note; a claim made from a
-- template copies them, so a later change leaves it as it was. Kept by version like the rest of the screen.
CREATE TABLE claim_template (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(255)   NOT NULL,
    name     VARCHAR(1000)  NOT NULL,
    amount   DECIMAL(19, 2) NOT NULL,
    note     VARCHAR(4000)  NOT NULL,
    UNIQUE (sheet_id, name)
);

CREATE TABLE claim_template_hist AS SELECT * FROM claim_template WITH NO DATA;
ALTER TABLE claim_template_hist ADD COLUMN hist_seq BIGINT NOT NULL;
ALTER TABLE claim_template_hist ADD COLUMN hist_made_on DATE NOT NULL;
ALTER TABLE claim_template_hist ADD COLUMN hist_gone BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX claim_template_hist_key ON claim_template_hist (id, hist_seq);
CREATE TRIGGER claim_template_history AFTER INSERT, UPDATE, DELETE ON claim_template FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
