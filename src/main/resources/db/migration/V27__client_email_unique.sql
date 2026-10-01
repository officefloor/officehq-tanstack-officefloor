-- Two clients cannot share an email. The column was already NOT NULL and format-checked
-- (V2__client_email_format.sql); this adds the uniqueness guard at the data layer so a duplicate
-- email can never be persisted, matching the server check in CreateClient.java. This is the final
-- line of defence behind the app-level existsByEmail check.
ALTER TABLE clients
    ADD CONSTRAINT clients_email_unique UNIQUE (email);
