-- A plan's pauses: paused on a day and, once resumed, resumed on a day (null while still paused). A plan date on or
-- after the day it was paused and before the day it was resumed is skipped: generate makes nothing for it.
CREATE TABLE plan_pause (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT NOT NULL REFERENCES plan (id),
    paused_on  DATE   NOT NULL,
    resumed_on DATE
);
CREATE INDEX plan_pause_plan ON plan_pause (plan_id);
