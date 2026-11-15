-- The sheet's holidays: a claim's due-by, fixed when it is created, that falls on one moves to the next
-- day that is not one. A holiday added later leaves existing due-by dates as they are.
CREATE TABLE holiday (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    holiday     DATE         NOT NULL,
    added_by    VARCHAR(200) NOT NULL,
    added_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT holiday_unique UNIQUE (sheet_id, holiday)
);
