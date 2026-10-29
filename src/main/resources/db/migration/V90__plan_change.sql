-- A plan's amount changes: the day it was changed (the sheet's today) and the new amount. The plan's own amount is the
-- latest one, used for the claims generated from then on; claims already generated keep theirs.
CREATE TABLE plan_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT         NOT NULL REFERENCES plan (id),
    changed_on DATE           NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL
);
CREATE INDEX plan_change_plan ON plan_change (plan_id);
