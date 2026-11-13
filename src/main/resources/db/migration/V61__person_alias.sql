-- A person's aliases, kept trimmed. Aliases share the names' uniqueness: no alias equals, ignoring case, another
-- alias or any person's name on the sheet.
CREATE TABLE person_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    alias VARCHAR(200) NOT NULL,
    alias_key VARCHAR(200) GENERATED ALWAYS AS (LOWER(alias))
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
