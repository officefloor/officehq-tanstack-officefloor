-- The people a claim is shared with, in the order shared: each holds a share (a percent with two
-- decimals, more than 0.00) of the claim's due; the claim's own person keeps the rest. A person holds
-- at most one share of a claim, and a claim's shares total at most 100.00.
CREATE TABLE claim_share (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    claim_id  BIGINT       NOT NULL REFERENCES claim (id),
    person_id BIGINT       NOT NULL,
    share     DECIMAL(5,2) NOT NULL
);
CREATE INDEX claim_share_sheet ON claim_share (sheet_id, claim_id, id);

CREATE TABLE asof.claim_share AS SELECT * FROM public.claim_share WHERE FALSE;
CREATE TABLE asof_hist.claim_share AS SELECT * FROM public.claim_share WHERE FALSE;
ALTER TABLE asof_hist.claim_share ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.claim_share ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.claim_share ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX claim_share_hist_sheet ON asof_hist.claim_share (sheet_id, h_seq);
CREATE TRIGGER claim_share_history AFTER INSERT, UPDATE, DELETE ON public.claim_share FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
