-- Each save of a person bumps its version; an edit names the version it loaded and is refused when
-- someone else has saved the person since.
ALTER TABLE person ADD COLUMN version INT DEFAULT 0 NOT NULL;
