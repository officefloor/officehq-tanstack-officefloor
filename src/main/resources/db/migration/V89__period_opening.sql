-- Each person's opening balance for a period: the sum of the amounts pasted for them and it, added to
-- their carried figure for it (live while it is open, fixed with the rest of the figure when it is closed).
CREATE TABLE period_opening (
    sheet_id    VARCHAR(200)  NOT NULL,
    period_name VARCHAR(200)  NOT NULL,
    person_id   BIGINT        NOT NULL,
    amount      DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (sheet_id, period_name, person_id)
);

CREATE TABLE asof.period_opening AS SELECT * FROM public.period_opening WHERE FALSE;
CREATE TABLE asof_hist.period_opening AS SELECT * FROM public.period_opening WHERE FALSE;
ALTER TABLE asof_hist.period_opening ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.period_opening ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.period_opening ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX period_opening_hist_sheet ON asof_hist.period_opening (sheet_id, h_seq);
CREATE TRIGGER period_opening_history AFTER INSERT, UPDATE, DELETE ON public.period_opening FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
