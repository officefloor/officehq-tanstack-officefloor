-- Each project (a "job") carries a short reference code, set when the job is created. No two jobs
-- may share a code, so the codes are a stable human-readable handle for a job. Nullable so rows that
-- predate the code (and seeds that omit one) stay valid; the UNIQUE constraint still forbids two
-- jobs sharing a non-null code (SQL treats NULLs as distinct, so multiple code-less rows are fine).
ALTER TABLE project ADD COLUMN code VARCHAR(255);
ALTER TABLE project ADD CONSTRAINT project_code_unique UNIQUE (code);
