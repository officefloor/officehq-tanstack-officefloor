-- A pool's member may be another pool of the sheet (member_pool, with person_id null) in place of a
-- person. A pool is a member of at most one pool, never of itself through any chain, and a chain of
-- pools is at most three deep.
ALTER TABLE public.pool_member DROP PRIMARY KEY;
ALTER TABLE public.pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE public.pool_member ADD COLUMN member_pool VARCHAR(200);
ALTER TABLE asof.pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE asof.pool_member ADD COLUMN member_pool VARCHAR(200);
ALTER TABLE asof_hist.pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE asof_hist.pool_member ADD COLUMN member_pool VARCHAR(200);
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.pool_member_history;
CREATE TRIGGER pool_member_history AFTER INSERT, UPDATE, DELETE ON public.pool_member FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
