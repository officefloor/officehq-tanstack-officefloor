-- Enforce at the schema level that every contact carries a proper (non-blank, structurally valid)
-- email address, matching the server (ContactsPost) and UI (ClientContactsPanel.isValidEmail) checks.
-- The LIKE pattern requires at least one char before '@', one between '@' and '.', and one after '.'.
ALTER TABLE contact ADD CONSTRAINT contact_email_valid CHECK (email LIKE '%_@_%._%');
