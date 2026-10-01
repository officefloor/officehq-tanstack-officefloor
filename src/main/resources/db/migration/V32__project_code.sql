-- Each project carries a short reference code, set when the project is created, and no two projects
-- may share one. The column is nullable so projects seeded before a code existed stay valid (and
-- H2's UNIQUE permits multiple NULLs); every project created through the app gets a non-blank code,
-- rejected as a duplicate server-side (CreateProject.java) before it reaches here. This UNIQUE
-- constraint is the final line of defence behind the app-level existsByCode check.
ALTER TABLE projects
    ADD COLUMN code VARCHAR(64);

ALTER TABLE projects
    ADD CONSTRAINT projects_code_unique UNIQUE (code);
