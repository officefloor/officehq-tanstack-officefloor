-- The sheet's notes: a text added by a user, kept with the sheet's today when it was added. The id orders
-- them as they were added; under an as-of date only those added on or before it show.
CREATE TABLE sheet_note (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(255)  NOT NULL,
    added_on   DATE          NOT NULL,
    user_name  VARCHAR(255)  NOT NULL,
    note_text  VARCHAR(4000) NOT NULL
);
CREATE INDEX sheet_note_sheet ON sheet_note (sheet_id, id);
