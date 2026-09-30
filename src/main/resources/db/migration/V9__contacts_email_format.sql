-- Every contact must have a PROPER email address too. The column was already NOT NULL (V8); this
-- adds a CHECK constraint as the last line of defence behind the front-end and server validation,
-- mirroring clients (V2): one @, non-empty local/domain parts, and a dotted domain.
ALTER TABLE contacts
    ADD CONSTRAINT contacts_email_format
    CHECK (REGEXP_LIKE(email, '^[^\s@]+@[^\s@]+\.[^\s@]+$'));
