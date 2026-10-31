-- Who last edited a claim (through the claim form or its row's amount entry); null while never edited.
-- Like its opener, its last editor may not settle or approve it.
ALTER TABLE public.claim ADD COLUMN edited_by VARCHAR(255);
ALTER TABLE asof.claim ADD COLUMN edited_by VARCHAR(255);
ALTER TABLE asof_hist.claim ADD COLUMN edited_by VARCHAR(255);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
