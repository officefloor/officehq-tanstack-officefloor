-- A plan's amount changes: on a day (the sheet's today) its amount became the new one. The plan's amount is its
-- latest; the claims generated from then on take it, those already generated keep theirs.
CREATE TABLE plan_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plan (id),
    changed_on DATE NOT NULL,
    amount DECIMAL(19, 2) NOT NULL
);
CREATE INDEX plan_change_plan ON plan_change (plan_id, id);
