-- A pool member may be another pool of the sheet (member_pool, its name; person_id NULL) instead of a person.
-- A pool is a member of at most one pool, never of itself through any chain, and a chain of pools is at most
-- three deep. A pool that is a member of another has no pot of its own: its pot is its cut in the pool above.
ALTER TABLE pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE pool_member ADD COLUMN member_pool VARCHAR(2000);
