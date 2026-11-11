-- Every role assigned on a sheet, oldest first, with the sheet's today when it was assigned: the
-- role a user had as of a date is the last one assigned to them on or before it (clerk when none).
-- What was already assigned before this history was kept counts from the beginning of time.
CREATE TABLE role_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    user_name VARCHAR(200) NOT NULL,
    role VARCHAR(20) NOT NULL,
    set_on DATE NOT NULL
);
CREATE INDEX role_history_sheet ON role_history(sheet_id, user_name, id);
INSERT INTO role_history (sheet_id, user_name, role, set_on)
    SELECT sheet_id, user_name, role, DATE '0001-01-01' FROM sheet_role;

-- The sheet's today when a delegation was made (null: from the beginning of time).
ALTER TABLE sheet_delegation ADD COLUMN made_on DATE;
