-- A client has one main (primary) contact. Flag it on the contact row: at most one contact per
-- client is the main one. Defaults to FALSE so existing contacts are "not the main contact" until
-- one is chosen; the server (ContactsPrimaryLogic) keeps the "one per client" invariant by clearing
-- the others when a new main contact is set. (`primary` is a SQL keyword, so the column is is_primary.)
ALTER TABLE contacts
    ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;
