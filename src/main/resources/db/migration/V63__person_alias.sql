-- A person may have aliases, stored trimmed. Aliases share the names' uniqueness: compared as names are (without
-- regard to case, ignoring surrounding spaces), no alias equals another alias or any name on the sheet.
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    person_id BIGINT       NOT NULL REFERENCES person(id),
    alias     VARCHAR(200) NOT NULL,
    alias_key VARCHAR(200) GENERATED ALWAYS AS (LOWER(TRIM(alias))),
    made_on   DATE
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias(sheet_id, alias_key);
CREATE INDEX person_alias_person ON person_alias(sheet_id, person_id);
