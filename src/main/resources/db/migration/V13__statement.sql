-- A person's statements: each records the person's due-total when it was taken (null when it was
-- n/a) and the sheet's today then. The latest (highest id) is the person's last statement.
CREATE TABLE statement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)   NOT NULL REFERENCES sheet (id),
    person_id BIGINT         NOT NULL REFERENCES person (id),
    due_total DECIMAL(14, 2),
    taken_on  DATE           NOT NULL
);
CREATE INDEX statement_person ON statement (sheet_id, person_id, id);
-- A claim bound by its person's last statement: it shows and totals stated_due (null: n/a) instead
-- of its live figure, until the next statement rebinds or unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(14, 2);
