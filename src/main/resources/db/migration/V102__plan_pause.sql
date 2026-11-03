-- A plan's pauses: paused from a day (the sheet's today when paused) until the day it was resumed (null while
-- still paused). A plan date on or after paused_on and before resumed_on is skipped: generate makes nothing for
-- it and it is left out of the plan's next dates. At most one pause of a plan is open (resumed_on null).
CREATE TABLE plan_pause (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL REFERENCES sheet (id),
    plan_id    BIGINT       NOT NULL REFERENCES plan (id),
    paused_on  DATE         NOT NULL,
    resumed_on DATE
);
CREATE INDEX plan_pause_plan ON plan_pause (plan_id, id);
CREATE INDEX plan_pause_sheet ON plan_pause (sheet_id);
