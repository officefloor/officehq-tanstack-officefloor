-- A person's aliases, kept without surrounding spaces. They share the names' uniqueness on the sheet, compared
-- without regard to case: alias_key is the lower-cased alias, unique on the sheet (an alias equal to a name is
-- refused by the app). The quick-add bar and the paste box find a person by an alias as well as by the name.
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT       NOT NULL REFERENCES person (id),
    sheet_id  VARCHAR(255) NOT NULL,
    alias     VARCHAR(255) NOT NULL,
    alias_key VARCHAR(255) GENERATED ALWAYS AS (LOWER(alias))
);
ALTER TABLE person_alias ADD CONSTRAINT person_alias_sheet_key UNIQUE (sheet_id, alias_key);
CREATE INDEX person_alias_person ON person_alias (person_id);
