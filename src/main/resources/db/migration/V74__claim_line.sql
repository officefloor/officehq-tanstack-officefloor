-- A DRAFT claim's lines, in the order added: each a label, a whole quantity (at least 1) and a unit
-- amount (two decimals). A claim with lines has the quantities times the unit amounts as its amount.
CREATE TABLE claim_line (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)  NOT NULL,
    claim_id BIGINT        NOT NULL REFERENCES claim (id),
    label    VARCHAR(2000) NOT NULL,
    qty      INT           NOT NULL,
    unit     DECIMAL(19,2) NOT NULL
);
CREATE INDEX claim_line_sheet ON claim_line (sheet_id, claim_id, id);

CREATE TABLE asof.claim_line AS SELECT * FROM public.claim_line WHERE FALSE;
CREATE TABLE asof_hist.claim_line AS SELECT * FROM public.claim_line WHERE FALSE;
ALTER TABLE asof_hist.claim_line ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.claim_line ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.claim_line ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX claim_line_hist_sheet ON asof_hist.claim_line (sheet_id, h_seq);
CREATE TRIGGER claim_line_history AFTER INSERT, UPDATE, DELETE ON public.claim_line FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
