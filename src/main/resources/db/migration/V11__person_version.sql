-- A person's version: 0 when added, one more on each stored edit. An edit names the version it was
-- made from and is refused when the person has been saved since (another user's edit in between).
ALTER TABLE person ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
