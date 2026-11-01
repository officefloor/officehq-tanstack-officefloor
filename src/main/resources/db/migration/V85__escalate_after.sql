-- The sheet's escalation gap: reminding a claim whose previous reminder was at least this many days
-- before the sheet's today escalates it (once per claim ever). Null when unset (off). Not dated.
ALTER TABLE public.sheet_setting ADD COLUMN escalate_after INT;
ALTER TABLE asof.sheet_setting ADD COLUMN escalate_after INT;
ALTER TABLE asof_hist.sheet_setting ADD COLUMN escalate_after INT;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.sheet_setting_history;
CREATE TRIGGER sheet_setting_history AFTER INSERT, UPDATE, DELETE ON public.sheet_setting FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
-- Whether the claim has been escalated: a claim is escalated at most once, ever.
ALTER TABLE public.claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof_hist.claim ADD COLUMN escalated BOOLEAN DEFAULT FALSE NOT NULL;
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
