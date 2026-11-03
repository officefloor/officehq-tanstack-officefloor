-- A person's aliases: other names they are known by on the sheet. An alias shares the names' uniqueness
-- (ignoring case, no alias equals any name or alias on the sheet), checked when one is added.
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    alias     VARCHAR(500) NOT NULL
);
CREATE INDEX person_alias_sheet ON person_alias (sheet_id, person_id);
