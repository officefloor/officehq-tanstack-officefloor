-- A person's name is stored without surrounding spaces, and is unique on its sheet ignoring case
-- (checked by the app before saving).
UPDATE person SET name = TRIM(name);
