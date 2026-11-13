-- Terms set for a grade: a claim whose person has that grade on its date is due by its date plus them. A grade
-- without its own terms uses the sheet's terms.
CREATE TABLE grade_terms (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    grade VARCHAR(1) NOT NULL,
    terms INT NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
-- A claim's due-by, fixed when it is created: its date plus the terms of its person's grade in force on its date.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD(DAY, (SELECT s.terms FROM sheet s WHERE s.id = claim.sheet_id), claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
