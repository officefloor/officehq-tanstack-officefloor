-- A client has one MAIN contact. Flag it on the contact row: is_primary is TRUE for the single
-- contact that is the client's main point of contact, FALSE for the rest. NOT NULL with a FALSE
-- default so an existing or freshly created contact is "not the main one" until explicitly chosen,
-- and the app never has to cope with a NULL. Which contact is primary is changed in-place (set one
-- TRUE, clear the siblings), so no separate table is needed — the flag lives beside the contact.
ALTER TABLE contacts
    ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;
