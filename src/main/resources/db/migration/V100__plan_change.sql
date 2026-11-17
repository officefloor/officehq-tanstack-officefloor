-- A plan's changes of amount, in the order made: changed_on is the sheet's today when it was changed. The plan's amount
-- is that of its last change (the plan's own amount while it has none); claims already made keep theirs.
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT         NOT NULL REFERENCES plan (id),
    changed_on DATE           NOT NULL,
    amount     DECIMAL(17, 2) NOT NULL
);
CREATE INDEX plan_change_plan ON plan_change (plan_id, id);
