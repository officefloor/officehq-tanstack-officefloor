-- A person's aliases: trimmed, each unique on the sheet (ignoring case) among all names and aliases.
CREATE TABLE person_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    alias VARCHAR(200) NOT NULL
);
CREATE INDEX person_alias_sheet ON person_alias(sheet_id);
