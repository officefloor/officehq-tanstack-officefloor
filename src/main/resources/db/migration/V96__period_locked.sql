-- Whether a steward has locked the period: a locked period cannot be reopened.
ALTER TABLE public.period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof.period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE asof_hist.period ADD COLUMN locked BOOLEAN DEFAULT FALSE NOT NULL;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.period_history;
CREATE TRIGGER period_history AFTER INSERT, UPDATE, DELETE ON public.period FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
