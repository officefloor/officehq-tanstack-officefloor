-- Two clients cannot share an email. Enforce uniqueness at the DB level too (the last line of
-- defence), matching the duplicate check in the server (ClientsPostLogic) and the error the front-end
-- surfaces. Applies to every client row regardless of archived state: an email already on file — even
-- an archived one — cannot be reused for a new client.
ALTER TABLE clients
    ADD CONSTRAINT clients_email_unique UNIQUE (email);
