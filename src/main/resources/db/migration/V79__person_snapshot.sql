-- A person's snapshots, in the order written: one per SNAPSHOT line written for them when the sheet's
-- today moved, each with the day being left and their due-total then (null when it was n/a).
CREATE TABLE person_snapshot (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    person_id BIGINT        NOT NULL,
    left_day  DATE          NOT NULL,
    due_total DECIMAL(14,2)
);
CREATE INDEX person_snapshot_sheet ON person_snapshot (sheet_id, person_id, id);

CREATE TABLE asof.person_snapshot AS SELECT * FROM public.person_snapshot WHERE FALSE;
CREATE TABLE asof_hist.person_snapshot AS SELECT * FROM public.person_snapshot WHERE FALSE;
ALTER TABLE asof_hist.person_snapshot ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.person_snapshot ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.person_snapshot ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX person_snapshot_hist_sheet ON asof_hist.person_snapshot (sheet_id, h_seq);
CREATE TRIGGER person_snapshot_history AFTER INSERT, UPDATE, DELETE ON public.person_snapshot FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
