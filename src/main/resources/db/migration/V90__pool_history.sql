-- So a pool member's cut can be shown as it stood at the end of a past period: the day (the sheet's today) each pool,
-- person member and pool member was added (null for one added before this was kept, there from the beginning of
-- time), and every value the pot has been set to, in order, with the sheet's today it was set on. The pot in force on
-- a date is the last one set on or before it; a sheet whose pot was set before this was kept has it from the
-- beginning of time.
ALTER TABLE pool ADD COLUMN added_on DATE;
ALTER TABLE pool_member ADD COLUMN added_on DATE;
ALTER TABLE pool_sub ADD COLUMN added_on DATE;
CREATE TABLE pot_change (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet     VARCHAR(200) NOT NULL,
    set_on    DATE         NOT NULL,
    pot_value VARCHAR(200) NOT NULL
);
CREATE INDEX pot_change_sheet ON pot_change (sheet, set_on, id);
INSERT INTO pot_change (sheet, set_on, pot_value)
    SELECT sheet, DATE '0001-01-01', setting_value FROM setting WHERE setting_key = 'pot';
