-- Each value the sheet's pot was set to, in the order set, with the sheet's today then (made_on; null: set before
-- this was kept, counting on every date): the pot as of a date is the last one set on or before it (none if none).
CREATE TABLE pot_log (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100)   NOT NULL REFERENCES sheet(id),
    val      DECIMAL(14, 2),
    made_on  DATE
);
CREATE INDEX pot_log_sheet ON pot_log(sheet_id, id);
INSERT INTO pot_log (sheet_id, val, made_on) SELECT id, pot, NULL FROM sheet WHERE pot IS NOT NULL;
