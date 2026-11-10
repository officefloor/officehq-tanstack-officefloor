-- The history the as-of view reads, on top of that kept for a statement taken as of a date: every
-- change below is kept with the sheet's today when it was made (set_on / added_on / archived_on), so
-- the screen as it stood at the end of a date shows only the changes made while the sheet's today
-- was on or before it. What was already stored before this history was kept counts from the
-- beginning of time (0001-01-01, or null).

-- The sheet's today when it was archived (null: not archived, or from the beginning of time).
ALTER TABLE sheet ADD COLUMN archived_on DATE;

-- Every state a claim has been saved in, oldest first: the claim as it stood at the end of a date
-- is its last one kept on or before it. A claim with none on or before a date was not yet added.
CREATE TABLE claim_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    person_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    claim_date DATE NOT NULL,
    note VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    claim_number VARCHAR(200),
    settled_due DECIMAL(19, 2),
    opened_by VARCHAR(200),
    stated BOOLEAN NOT NULL,
    stated_due DECIMAL(19, 2),
    corrects VARCHAR(200),
    due_by DATE NOT NULL,
    deferred_until DATE,
    proofs INT NOT NULL,
    split_from VARCHAR(200),
    late_entry BOOLEAN NOT NULL,
    listed BIGINT,
    set_on DATE NOT NULL
);
CREATE INDEX claim_history_sheet ON claim_history(sheet_id, set_on, id);
INSERT INTO claim_history (sheet_id, claim_id, person_id, amount, claim_date, note, status, claim_number,
        settled_due, opened_by, stated, stated_due, corrects, due_by, deferred_until, proofs, split_from,
        late_entry, listed, set_on)
    SELECT sheet_id, id, person_id, amount, claim_date, note, status, claim_number, settled_due, opened_by,
        stated, stated_due, corrects, due_by, deferred_until, proofs, split_from, late_entry, listed,
        DATE '0001-01-01' FROM claim;

-- Every change to a person's statements, oldest first: their count, last due-total and date then.
CREATE TABLE person_statement_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    statement_count INT NOT NULL,
    stated_total VARCHAR(30),
    statement_date DATE,
    set_on DATE NOT NULL
);
CREATE INDEX person_statement_history_sheet ON person_statement_history(sheet_id, person_id, id);
INSERT INTO person_statement_history (sheet_id, person_id, statement_count, stated_total, statement_date, set_on)
    SELECT sheet_id, id, statement_count, stated_total, statement_date, DATE '0001-01-01' FROM person
    WHERE statement_count > 0;

-- The sheet's today when a patron was named (null: from the beginning of time).
ALTER TABLE person_patron ADD COLUMN added_on DATE;

-- The sheet's today when a period was added (null: from the beginning of time), and every state it
-- has been closed, soft-closed or reopened in, oldest first.
ALTER TABLE period ADD COLUMN added_on DATE;
CREATE TABLE period_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    name VARCHAR(200) NOT NULL,
    closed BOOLEAN NOT NULL,
    soft BOOLEAN NOT NULL,
    closed_total DECIMAL(19, 2),
    set_on DATE NOT NULL
);
CREATE INDEX period_history_sheet ON period_history(sheet_id, id);
INSERT INTO period_history (sheet_id, name, closed, soft, closed_total, set_on)
    SELECT sheet_id, name, closed, soft, closed_total, DATE '0001-01-01' FROM period WHERE closed;
