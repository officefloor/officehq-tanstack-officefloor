-- A claim bound by its person's last statement: stated is true while bound, and stated_due is the figure it
-- was bound to (null when its grade had no rate then). A statement rebinds every claim of the person.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(15, 2);
-- The statements taken of a person: their due-total then (null when n/a) and the sheet's today then.
CREATE TABLE statement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet     VARCHAR(200)   NOT NULL,
    person    BIGINT         NOT NULL REFERENCES person (id),
    due_total DECIMAL(15, 2),
    taken_on  DATE           NOT NULL
);
CREATE INDEX statement_sheet ON statement (sheet, person, id);
