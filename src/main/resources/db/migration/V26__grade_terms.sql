-- A grade's own terms on a sheet: a claim of a person of that grade (in force on the claim's date) is due this
-- many days after its date. A grade with no row uses the sheet's terms.
CREATE TABLE sheet_grade_terms (
    sheet_id   VARCHAR(200) NOT NULL,
    grade      CHAR(1)      NOT NULL,
    terms_days INT          NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);

-- A claim's due-by, fixed when it is created: its date plus the terms of its person's grade in force on that date.
-- It does not change when the terms or the claim's date change later.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD('DAY', COALESCE((SELECT s.terms_days FROM sheet s WHERE s.id = c.sheet_id), 14), c.claim_date);
