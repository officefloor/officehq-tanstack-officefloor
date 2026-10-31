-- How many times a person has been saved since added. An edit names the version it loaded and is
-- refused when the person has been saved since (someone else's edit in the meantime).
ALTER TABLE person ADD COLUMN version INT NOT NULL DEFAULT 0;
