-- How many of a claim's proofs are photos: the rest of its proofs are receipts (a proof added
-- without a kind is a receipt).
ALTER TABLE public.claim ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
ALTER TABLE asof.claim ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
ALTER TABLE asof_hist.claim ADD COLUMN proof_photos INT DEFAULT 0 NOT NULL;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
-- The sheet's proof kinds: a comma separated list of kinds (receipt, photo); while set, a claim above
-- the proof threshold needs a proof of each before it can be settled or approved. Null when unset. Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN proof_kinds VARCHAR(64);
ALTER TABLE asof.sheet_setting ADD COLUMN proof_kinds VARCHAR(64);
ALTER TABLE asof_hist.sheet_setting ADD COLUMN proof_kinds VARCHAR(64);
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
