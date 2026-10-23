-- A plan's amount changes: the new amount, on the sheet's today then, and by whom. plan.amount holds the plan's current
-- amount, which the claims generated from then on are made of; claims already generated keep theirs.
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT         NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
    changed_on DATE           NOT NULL,
    amount     DECIMAL(14, 2) NOT NULL,
    changed_by VARCHAR(100)   NOT NULL
);
CREATE INDEX plan_change_plan ON plan_change(plan_id);
