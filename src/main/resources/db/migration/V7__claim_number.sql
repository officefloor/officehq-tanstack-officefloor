-- A claim's number on its sheet: given when it is opened (1, 2, ... per sheet, never reused) and
-- cleared when it is voided. Drafts have none.
ALTER TABLE claim ADD COLUMN number_on_sheet INT;
-- The last number given on each sheet; a sheet with no row has given none.
CREATE TABLE claim_number (
    sheet       VARCHAR(200) PRIMARY KEY,
    last_number INT NOT NULL
);
