-- A plan's pauses, in the order made: paused_on is the sheet's today when it was paused, resumed_on the sheet's today
-- when it was resumed (null while it stays paused). A plan date on or after paused_on and before resumed_on is skipped.
-- A plan has at most one pause with a null resumed_on: it is then paused.
CREATE TABLE plan_pause (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT NOT NULL REFERENCES plan (id),
    paused_on  DATE   NOT NULL,
    resumed_on DATE
);
CREATE INDEX plan_pause_plan ON plan_pause (plan_id, id);
