-- Terms set per grade: a claim of a person of that grade is due by its date plus them. A grade with
-- no row uses the sheet's terms.
CREATE TABLE sheet_grade_terms (
    sheet_id VARCHAR(200) NOT NULL,
    grade    VARCHAR(1)   NOT NULL,
    terms    INT          NOT NULL,
    PRIMARY KEY (sheet_id, grade)
);
-- A claim's due-by is fixed when it is added, from the terms in force then; it does not follow later
-- changes to the terms or to the claim's date. Claims already kept are fixed by the sheet's terms now.
ALTER TABLE claim ADD COLUMN due_by DATE;
UPDATE claim c SET due_by = DATEADD('DAY',
    COALESCE((SELECT s.terms FROM sheet_setting s WHERE s.sheet_id = c.sheet_id), 14), c.claim_date);
ALTER TABLE claim ALTER COLUMN due_by SET NOT NULL;
