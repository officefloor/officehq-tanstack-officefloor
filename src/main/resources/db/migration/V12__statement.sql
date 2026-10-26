-- A claim bound by its person's last statement: stated, at the figure it had then (stated_due; null when it
-- had none). It shows and totals that figure until the person's next statement, which rebinds every claim
-- of theirs that counts then and leaves the rest unbound.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(15, 2);

-- The statements taken of a person, in the order taken (by id): their due-total then ("n/a" when it had
-- none), how many of their claims counted, and the sheet's today when it was taken.
CREATE TABLE statement (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255) NOT NULL,
    person_id  BIGINT       NOT NULL REFERENCES person (id),
    due_total  VARCHAR(32)  NOT NULL,
    claims     INT          NOT NULL,
    stated_on  DATE         NOT NULL
);
CREATE INDEX statement_sheet ON statement (sheet_id, person_id, id);
