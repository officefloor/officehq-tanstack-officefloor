-- The sheet's pot each time it was set, with the sheet's today then (on_date; null: from the beginning of time), so a
-- pool's cuts can be taken as they stood at the end of an earlier period: the pot on a date is the one set last on or
-- before it (null pot: none). The pot already set is taken as set from the beginning of time.
CREATE TABLE sheet_pot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    on_date DATE,
    pot DECIMAL(15, 2)
);
CREATE INDEX sheet_pot_sheet ON sheet_pot (sheet_id);
INSERT INTO sheet_pot (sheet_id, on_date, pot) SELECT id, NULL, pot FROM sheet WHERE pot IS NOT NULL;
-- The sheet's today when each pool, pool member, crew, crew member and hold was added (null: from the beginning of
-- time): one added after a date was not there on it.
ALTER TABLE pool ADD COLUMN added_on DATE;
ALTER TABLE pool_member ADD COLUMN added_on DATE;
ALTER TABLE crew ADD COLUMN added_on DATE;
ALTER TABLE crew_member ADD COLUMN added_on DATE;
ALTER TABLE person_hold ADD COLUMN added_on DATE;
