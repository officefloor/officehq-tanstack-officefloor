-- A plan's pauses: paused on a day and resumed on a later one (null while still paused). A plan date on or after
-- paused_on and before resumed_on is skipped, never made.
CREATE TABLE plan_pause (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id    BIGINT NOT NULL,
    paused_on  DATE   NOT NULL,
    resumed_on DATE
);
