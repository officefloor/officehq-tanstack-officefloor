-- A sheet's terms per grade: the days after a claim's date that a claim of that grade is due by.
-- A grade with no row uses the sheet's terms.
CREATE TABLE grade_terms (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    grade CHAR(1) NOT NULL,
    terms INT NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
-- A claim's due-by, fixed when it is made: its date plus the terms of its person's grade in force on
-- its date. It does not move when the terms or the claim's date change later.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim SET due_by = DATEADD('DAY', (SELECT s.terms FROM sheet s WHERE s.id = claim.sheet_id), claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
