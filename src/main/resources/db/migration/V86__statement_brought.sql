-- A statement a merge or a transfer brought over from another person who already had their own: shown among the
-- person's statements, but never their last statement while one of their own stands.
ALTER TABLE person_statement ADD COLUMN brought BOOLEAN DEFAULT FALSE NOT NULL;
