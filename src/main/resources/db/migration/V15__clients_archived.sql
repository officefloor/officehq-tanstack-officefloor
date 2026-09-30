-- Archiving a client: instead of deleting it, mark it archived so it drops off the list and search
-- but is retained. Existing clients default to not archived. The clients list (and its search, which
-- filters that same list) never shows archived clients.
ALTER TABLE clients ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
