-- A plan's pauses: paused on the sheet's today then, resumed on the sheet's today then (empty while it is still paused).
-- A plan date on or after the day a pause began and before the day it was resumed (any date on or after it, while
-- still paused) is skipped, never generated, not postponed. At most one pause of a plan is open at a time.
CREATE TABLE plan_pause (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT       NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
    paused_on  DATE         NOT NULL,
    resumed_on DATE,
    paused_by  VARCHAR(100) NOT NULL,
    resumed_by VARCHAR(100),
    CHECK (resumed_on IS NULL OR resumed_on >= paused_on)
);
CREATE INDEX plan_pause_plan ON plan_pause(plan_id);
