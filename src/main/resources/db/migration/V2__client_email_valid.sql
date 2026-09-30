-- Enforce at the schema level that every client carries a proper (non-blank, structurally valid)
-- email address, matching the server (ClientsPost) and UI (ClientsPage.isValidEmail) checks. The
-- LIKE pattern requires at least one char before '@', one between '@' and '.', and one after '.'.
ALTER TABLE client ADD CONSTRAINT client_email_valid CHECK (email LIKE '%_@_%._%');
