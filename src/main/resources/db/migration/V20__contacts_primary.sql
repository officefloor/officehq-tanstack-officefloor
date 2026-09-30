-- A client has one MAIN contact. Mark it on the contact row: exactly one contact per client is the
-- primary. Additive and defaulted so existing rows stay valid; the app keeps at most one primary per
-- client by clearing the others whenever a new one is chosen.
ALTER TABLE contacts
    ADD COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE;
