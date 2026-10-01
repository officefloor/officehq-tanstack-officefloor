-- Every contact must have a PROPER email address. The column was already NOT NULL; this adds the
-- format guard at the data layer so a malformed or blank email can never be persisted, matching the
-- UI (contactForm.slot.tsx) and server (CreateContact.java) checks. Regex: local@domain.tld, no
-- whitespace — the same shape clients use (V2__client_email_format.sql).
ALTER TABLE contacts
    ADD CONSTRAINT contacts_email_format
    CHECK (REGEXP_LIKE(email, '^[^@\s]+@[^@\s]+\.[^@\s]+$'));
