-- Every client must have a PROPER email address. The column was already NOT NULL; this adds the
-- format guard at the data layer so a malformed or blank email can never be persisted, matching the
-- UI (ClientForm.tsx) and server (CreateClient.java) checks. Regex: local@domain.tld, no whitespace.
ALTER TABLE clients
    ADD CONSTRAINT clients_email_format
    CHECK (REGEXP_LIKE(email, '^[^@\s]+@[^@\s]+\.[^@\s]+$'));
