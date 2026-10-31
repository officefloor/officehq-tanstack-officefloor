-- A person's aliases: other names they go by, kept trimmed. An alias shares the names' uniqueness on
-- its sheet (compared without case and ignoring surrounding spaces): it equals no name or alias there.
-- `alias_key` is the form they are compared in.
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    alias     VARCHAR(500) NOT NULL,
    alias_key VARCHAR(500) NOT NULL
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);

CREATE TABLE asof.person_alias AS SELECT * FROM public.person_alias WHERE FALSE;
CREATE TABLE asof_hist.person_alias AS SELECT * FROM public.person_alias WHERE FALSE;
ALTER TABLE asof_hist.person_alias ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_alias ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_alias ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_alias_hist_sheet ON asof_hist.person_alias (sheet_id, h_seq);
CREATE TRIGGER person_alias_history AFTER INSERT, UPDATE, DELETE ON public.person_alias FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
