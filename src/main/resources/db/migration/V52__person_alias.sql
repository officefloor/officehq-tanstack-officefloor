-- A person's aliases: trimmed, each unique on the sheet without regard to case, and never another name on it
-- (checked when names and aliases are written); alias_key is the alias as compared.
CREATE TABLE person_alias (
    sheet     VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    alias     VARCHAR(200) NOT NULL,
    alias_key VARCHAR(200) GENERATED ALWAYS AS (LOWER(alias)),
    PRIMARY KEY (sheet, person_id, alias)
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias (sheet, alias_key);
