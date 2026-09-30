-- A project's short reference code, set when the project is created and shown on its row. Kept
-- unique so no two projects share one: the UNIQUE constraint is the last line of defence behind the
-- front-end and server validation, rejecting a duplicate even if those checks are bypassed. Applied
-- to a fresh schema, so no existing rows conflict (the column is nullable; H2 treats NULLs as
-- distinct, so pre-existing code-less rows would not collide).
ALTER TABLE projects ADD COLUMN code VARCHAR(64);

ALTER TABLE projects
    ADD CONSTRAINT projects_code_unique UNIQUE (code);
