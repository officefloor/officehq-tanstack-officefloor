-- A plan's pauses: paused on the sheet's today, resumed on the sheet's today (null: still paused). A
-- plan date on or after the day it was paused and before the day it was resumed is skipped, not
-- postponed: generate makes nothing for it and the plan's next dates leave it out.
CREATE TABLE plan_pause (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plan(id),
    paused_on DATE NOT NULL,
    resumed_on DATE
);
