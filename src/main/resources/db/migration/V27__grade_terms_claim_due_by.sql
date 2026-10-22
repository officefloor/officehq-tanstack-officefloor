-- A grade's own terms (a number of days) on a sheet; a grade without a row uses the sheet's terms.
CREATE TABLE grade_terms (
    sheet_id   VARCHAR(100) NOT NULL REFERENCES sheet(id),
    grade      CHAR(1)      NOT NULL CHECK (grade IN ('A', 'B', 'C')),
    terms_days INT          NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
-- A claim's due-by is fixed when it is created: its date plus the terms of its person's grade in force on its date
-- (else the sheet's terms). It does not change when the terms or the claim's date change later.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD(DAY, (SELECT s.terms_days FROM sheet s WHERE s.id = c.sheet_id), c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
