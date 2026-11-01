-- The sheet's crews, in the order added: a name (trimmed, at most once per sheet) and a lead, a person
-- of the sheet.
CREATE TABLE crew (
    sheet_id  VARCHAR(200) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    lead_id   BIGINT       NOT NULL,
    position  BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, name)
);

-- A crew's members, in the order added: a person of the sheet, not the crew's lead, at most once per
-- crew (a person may be in several crews).
CREATE TABLE crew_member (
    sheet_id  VARCHAR(200) NOT NULL,
    crew      VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    position  BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, crew, person_id)
);

CREATE TABLE asof.crew AS SELECT * FROM public.crew WHERE FALSE;
CREATE TABLE asof_hist.crew AS SELECT * FROM public.crew WHERE FALSE;
ALTER TABLE asof_hist.crew ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.crew ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.crew ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX crew_hist_sheet ON asof_hist.crew (sheet_id, h_seq);
CREATE TRIGGER crew_history AFTER INSERT, UPDATE, DELETE ON public.crew FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.crew_member AS SELECT * FROM public.crew_member WHERE FALSE;
CREATE TABLE asof_hist.crew_member AS SELECT * FROM public.crew_member WHERE FALSE;
ALTER TABLE asof_hist.crew_member ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.crew_member ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.crew_member ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX crew_member_hist_sheet ON asof_hist.crew_member (sheet_id, h_seq);
CREATE TRIGGER crew_member_history AFTER INSERT, UPDATE, DELETE ON public.crew_member FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
