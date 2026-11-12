-- A pool member may be another pool of the sheet instead of a person: member_pool_id is set and person_id
-- is null. A pool is a member of at most one pool, never of itself through any chain, and a chain of pools
-- is at most three deep (refused on adding, see ValidateMember). Kept in pool_member_hist too; the history
-- trigger is set again afterwards, as in V45__claim_tags.sql.
DROP TRIGGER pool_member_history;
ALTER TABLE pool_member ALTER COLUMN person_id SET NULL;
ALTER TABLE pool_member ADD COLUMN member_pool_id BIGINT;
ALTER TABLE pool_member ADD CONSTRAINT pool_member_pool_unique UNIQUE (member_pool_id);
ALTER TABLE pool_member_hist ALTER COLUMN person_id SET NULL;
ALTER TABLE pool_member_hist ADD COLUMN member_pool_id BIGINT;
CREATE TRIGGER pool_member_history AFTER INSERT, UPDATE, DELETE ON pool_member FOR EACH ROW
    CALL 'net.officefloor.hq.app.history.HistoryTrigger';
