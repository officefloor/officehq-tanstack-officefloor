-- A person's statement: their due-total when it was taken (n/a when there was none) and the sheet's
-- today then. Their count and last one are read from here.
CREATE TABLE statement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    due_total VARCHAR(30) NOT NULL,
    taken_on DATE NOT NULL
);
CREATE INDEX statement_person ON statement (person_id, id);
-- A claim bound by its person's last statement shows and totals the figure it had then (stated_due;
-- null when it had none, n/a), until the next statement rebinds or unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(19, 2);
