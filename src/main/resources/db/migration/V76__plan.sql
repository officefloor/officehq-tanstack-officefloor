-- A person's plans: an amount, a start date, every N days (at least 1) and an end date (not before the start). Each plan
-- date (the start, then every N days, up to the end) gets at most one generated claim; plan_claim keeps which dates have
-- one, so generating again makes none for them. A plan goes with its person (its sheet is the person's).
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id  BIGINT         NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    amount     DECIMAL(14, 2) NOT NULL,
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL CHECK (every_days >= 1),
    end_date   DATE           NOT NULL,
    added_by   VARCHAR(100)   NOT NULL,
    CHECK (end_date >= start_date)
);
CREATE INDEX plan_person ON plan(person_id);
CREATE TABLE plan_claim (
    plan_id  BIGINT NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
    on_date  DATE   NOT NULL,
    claim_id BIGINT NOT NULL,
    PRIMARY KEY (plan_id, on_date)
);
