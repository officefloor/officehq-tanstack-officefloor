-- The dashboard measures "overdue" against a fixed reference date rather than the wall clock, so the
-- overdue count is deterministic under test. That reference date ("as of") lives in a single-row
-- app_clock table: the harness seed sets it, and the dashboard's overdue endpoint reads it (falling
-- back to the real current date when none is set). A single row is expected, keyed on id = 1.
CREATE TABLE app_clock (
    id INT PRIMARY KEY,
    as_of DATE NOT NULL
);
