-- A claim's comments, in the order written: each keeps who wrote it, the sheet's today then, and its text.
CREATE TABLE claim_comment (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL,
    claim_id BIGINT        NOT NULL REFERENCES claim (id),
    by_user  VARCHAR(200)  NOT NULL,
    on_date  DATE          NOT NULL,
    comment_text VARCHAR(2000) NOT NULL
);
CREATE INDEX claim_comment_sheet ON claim_comment (sheet_id, claim_id, id);

CREATE TABLE asof.claim_comment AS SELECT * FROM public.claim_comment WHERE FALSE;
CREATE TABLE asof_hist.claim_comment AS SELECT * FROM public.claim_comment WHERE FALSE;
ALTER TABLE asof_hist.claim_comment ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.claim_comment ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.claim_comment ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX claim_comment_hist_sheet ON asof_hist.claim_comment (sheet_id, h_seq);
CREATE TRIGGER claim_comment_history AFTER INSERT, UPDATE, DELETE ON public.claim_comment FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
