-- A plan's pauses: paused on a day and (once resumed) resumed on a day. A plan date on or after the day it was paused
-- and before the day it was resumed (or with resumed_on null: any date from the pause on) is skipped, not postponed.
CREATE TABLE person_plan_pause (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    plan_id BIGINT NOT NULL REFERENCES person_plan (id),
    paused_on DATE NOT NULL,
    resumed_on DATE
);
CREATE INDEX person_plan_pause_plan ON person_plan_pause (plan_id);
