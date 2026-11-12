-- A person's statements: each records their due-total at that moment (null when it was n/a), how
-- many of their claims counted then, and the sheet's today when it was taken. Listed by id.
CREATE TABLE statement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(255)   NOT NULL,
    person_id BIGINT         NOT NULL,
    taken_on  DATE           NOT NULL,
    due_total DECIMAL(19, 2),
    claims    INT            NOT NULL,
    taken_by  VARCHAR(255)
);
CREATE INDEX statement_person ON statement (sheet_id, person_id, id);
-- A claim bound by its person's last statement: it shows and totals stated_due (the figure it had
-- then; null when it had none, n/a) until their next statement rebinds or unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(19, 2);
