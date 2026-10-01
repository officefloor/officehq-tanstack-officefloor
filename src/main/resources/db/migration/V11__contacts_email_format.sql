-- A contact must have a proper email address, not just a non-null one. Enforce the shape at the DB
-- level too (local part, @, a domain with a dot), matching the server (ContactsPostLogic) +
-- front-end validation: rejects '' and values with no @/domain, so no bad row can slip in through
-- any path.
ALTER TABLE contacts
    ADD CONSTRAINT contacts_email_format CHECK (email LIKE '%_@_%._%');
