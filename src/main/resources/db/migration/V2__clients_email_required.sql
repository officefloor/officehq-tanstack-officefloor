-- A client must have a proper email address, not just a non-null one. Enforce the shape at the DB
-- level too (local part, @, a domain with a dot), matching the server + front-end validation:
-- rejects '' and values with no @/domain, so no bad row can slip in through any path.
ALTER TABLE clients
    ADD CONSTRAINT clients_email_format CHECK (email LIKE '%_@_%._%');
