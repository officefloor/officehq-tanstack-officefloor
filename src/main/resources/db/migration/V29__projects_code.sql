-- Each project (job) carries a short reference code, set when the job is created. The code is
-- unique across projects so no two jobs share one — enforce that at the DB level (the last line of
-- defence), matching the duplicate check in the server (ProjectsPostLogic) and the error the
-- front-end surfaces. Nullable: projects predating this column have no code (multiple NULLs are
-- allowed under a UNIQUE constraint), so the uniqueness only binds the codes actually set.
ALTER TABLE projects
    ADD COLUMN code VARCHAR(255);

ALTER TABLE projects
    ADD CONSTRAINT projects_code_unique UNIQUE (code);
