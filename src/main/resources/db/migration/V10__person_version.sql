-- Goes up by one on every save of a person. An editor sends back the version it loaded; a save
-- whose version is no longer the stored one is refused (someone else saved in the meantime).
ALTER TABLE person ADD COLUMN version INT DEFAULT 0 NOT NULL;
