-- No two clients may share an email address. Adds a UNIQUE constraint as the last line of defence
-- behind the front-end and server validation: a duplicate insert is rejected by the DB even if the
-- checks above are bypassed. Applied to a fresh schema, so no existing rows conflict.
ALTER TABLE clients
    ADD CONSTRAINT clients_email_unique UNIQUE (email);
