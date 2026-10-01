-- Archiving (not deleting) a client: a client can be tucked away so it drops off the list and the
-- search while its row — and everything hanging off it — is kept. A boolean flag on the client
-- carries that state; existing clients default to not-archived, so the list is unchanged until one
-- is archived.
ALTER TABLE clients ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
