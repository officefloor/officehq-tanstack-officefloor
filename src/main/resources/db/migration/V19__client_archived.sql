-- Archiving a client tucks it away instead of deleting it: the row is kept but drops off the
-- default client list and search. A boolean flag on client, defaulting FALSE so every existing (and
-- newly created) client starts un-archived and visible.
ALTER TABLE client ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
