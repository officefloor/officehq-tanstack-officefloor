-- A plan's amount changes, oldest first: on a day (the sheet's today) the plan's amount became this one. The plan's
-- own amount is its current one; the claims it has already made keep the amount they were made with.
CREATE TABLE person_plan_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    plan_id BIGINT NOT NULL REFERENCES person_plan (id),
    changed_on DATE NOT NULL,
    amount DECIMAL(15, 2) NOT NULL
);
CREATE INDEX person_plan_change_plan ON person_plan_change (plan_id);
