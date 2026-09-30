-- No two clients may share an email address. Enforce it at the schema level (defense in depth
-- behind the ClientsPost check and the UI), so a duplicate can never be persisted regardless of the
-- path that tries. Existing rows are already distinct; the seed inserts one row per email.
ALTER TABLE client ADD CONSTRAINT client_email_unique UNIQUE (email);
