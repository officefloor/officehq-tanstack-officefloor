-- Whether an OPEN or PENDING claim is held: it then counts in no live total, unless a statement binds it.
ALTER TABLE public.claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof_hist.claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.claim_history;
CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON public.claim FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
