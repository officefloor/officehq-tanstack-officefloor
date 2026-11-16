-- A person's aliases: each kept without surrounding spaces; alias_key is the alias as names are compared (lower-cased).
-- An alias is unique on the sheet, and the app keeps it apart from every person's name there too.
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    alias     VARCHAR(500) NOT NULL,
    alias_key VARCHAR(500) GENERATED ALWAYS AS (LOWER(alias))
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);
