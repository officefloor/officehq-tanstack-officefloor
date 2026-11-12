-- A person's plans: a DRAFT claim of the amount is generated for every plan date (the start date, then
-- every every_days days) up to the end date and the sheet's today. plan_made holds each plan date a
-- claim was made for, so a date is made at most once and a refused one is tried again next time.
CREATE TABLE plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255)   NOT NULL,
    person_id  BIGINT         NOT NULL,
    amount     DECIMAL(19, 2) NOT NULL,
    start_date DATE           NOT NULL,
    every_days INT            NOT NULL,
    end_date   DATE           NOT NULL
);
CREATE INDEX plan_sheet ON plan (sheet_id, id);

CREATE TABLE plan_made (
    plan_id BIGINT NOT NULL,
    made_on DATE   NOT NULL,
    PRIMARY KEY (plan_id, made_on)
);
