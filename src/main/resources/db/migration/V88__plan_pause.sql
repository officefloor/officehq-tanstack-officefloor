-- A plan's pauses: each from the day it was paused (paused_on) to the day it was resumed (resumed_on, null
-- while still paused). A plan date on or after paused_on and before resumed_on is skipped, not postponed.
CREATE TABLE plan_pause (
    plan_id    BIGINT NOT NULL,
    paused_on  DATE   NOT NULL,
    resumed_on DATE
);
CREATE INDEX plan_pause_plan ON plan_pause (plan_id);
