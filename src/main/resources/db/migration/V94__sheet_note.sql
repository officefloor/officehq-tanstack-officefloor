-- The sheet's notes: each a text, who added it and the sheet's today when it was added; listed
-- newest first (by id).
CREATE TABLE sheet_note (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL,
    text     VARCHAR(1000) NOT NULL,
    by_user  VARCHAR(200)  NOT NULL,
    added_on DATE          NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet_id, id);

CREATE TABLE asof.sheet_note AS SELECT * FROM public.sheet_note WHERE FALSE;
CREATE TABLE asof_hist.sheet_note AS SELECT * FROM public.sheet_note WHERE FALSE;
ALTER TABLE asof_hist.sheet_note ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.sheet_note ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.sheet_note ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX sheet_note_hist_sheet ON asof_hist.sheet_note (sheet_id, h_seq);
CREATE TRIGGER sheet_note_history AFTER INSERT, UPDATE, DELETE ON public.sheet_note FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
