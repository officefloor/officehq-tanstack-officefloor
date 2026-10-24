-- A person's aliases: other names they go by, trimmed. An alias shares the names' uniqueness on the sheet: no alias
-- equals, ignoring case, any name or other alias on it.
CREATE TABLE person_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    alias VARCHAR(255) NOT NULL
);
CREATE INDEX person_alias_sheet ON person_alias (sheet_id, id);
