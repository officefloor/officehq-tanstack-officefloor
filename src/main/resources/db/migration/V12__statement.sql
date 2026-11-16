-- A claim's statement binding: stated is true while the claim is bound to the figure it had at its
-- person's last statement, and stated_due is that figure (null when it had none: its grade had no
-- rate). The person's next statement rebinds or unbinds it.
ALTER TABLE claim ADD COLUMN stated BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE claim ADD COLUMN stated_due DECIMAL(19, 2);

-- The statements taken for a person: the sheet's today when taken, the person's due-total then
-- (text: two decimals, or n/a) and how many of their claims counted. The id orders them as taken.
CREATE TABLE statement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    taken_on  DATE         NOT NULL,
    due_total VARCHAR(40)  NOT NULL,
    claims    INT          NOT NULL,
    taken_by  VARCHAR(500) NOT NULL
);
CREATE INDEX statement_sheet ON statement (sheet_id, person_id, id);
