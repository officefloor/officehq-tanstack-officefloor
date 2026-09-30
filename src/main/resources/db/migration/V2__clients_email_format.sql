-- Every client must have a PROPER email address. The column was already NOT NULL (V1); this adds a
-- CHECK constraint as the last line of defence behind the front-end and server validation, mirroring
-- their pattern: one @, non-empty local/domain parts, and a dotted domain.
ALTER TABLE clients
    ADD CONSTRAINT clients_email_format
    CHECK (REGEXP_LIKE(email, '^[^\s@]+@[^\s@]+\.[^\s@]+$'));
