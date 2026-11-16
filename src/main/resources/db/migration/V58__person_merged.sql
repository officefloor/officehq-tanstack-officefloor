-- A person merged into another, as they stood when merged, so a sheet read as of a date before the merge still lists
-- them: grades as "grade@from" joined by ";" (from empty: from the beginning), added_on as on person (null: there from
-- the beginning), merged_on the sheet's today when merged.
CREATE TABLE person_merged (
    id           BIGINT        PRIMARY KEY,
    sheet_id     VARCHAR(200)  NOT NULL,
    name         VARCHAR(500)  NOT NULL,
    grades       VARCHAR(8000) NOT NULL,
    retired_from DATE,
    terms        INT,
    contact      VARCHAR(500),
    added_on     DATE,
    merged_on    DATE          NOT NULL
);
CREATE INDEX person_merged_sheet ON person_merged (sheet_id, id);
