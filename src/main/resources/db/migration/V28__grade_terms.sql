-- A grade's own terms on a sheet: the days after a claim's date that a claim of a person of that grade
-- is due by. A grade with no row here uses the sheet's terms.
CREATE TABLE grade_terms (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade    VARCHAR(1)   NOT NULL,
    terms    INT          NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
-- A claim's due-by, fixed when it is created from the terms then in force; an existing claim takes its
-- date plus its sheet's terms.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD(DAY, (SELECT s.terms FROM sheet s WHERE s.id = c.sheet_id), c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
