-- A plan's amount changes: on the sheet's today (changed_on) the plan's amount became amount. The plan's own
-- amount is the latest; claims already generated keep theirs. Listed oldest first (by id).
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT        NOT NULL,
    changed_on DATE          NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL
);
CREATE INDEX plan_change_plan ON plan_change (plan_id);
