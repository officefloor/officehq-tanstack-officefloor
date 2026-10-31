-- A plan's amount changes, in the order made: on the sheet's today the plan's amount became amount. plan.amount holds the
-- latest, which the claims generated from then on take; claims already generated keep theirs.
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT        NOT NULL,
    changed_on DATE          NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL
);
