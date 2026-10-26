-- A person's aliases: other names they go by, stored trimmed. An alias shares the names' uniqueness on its sheet,
-- ignoring case and surrounding spaces: no alias equals any name or other alias there (the app refuses one with a
-- message; this index is the backstop against another alias).
CREATE TABLE person_alias (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    person_id BIGINT       NOT NULL REFERENCES person (id),
    alias     VARCHAR(200) NOT NULL,
    alias_key VARCHAR(200) GENERATED ALWAYS AS (LOWER(TRIM(alias)))
);
CREATE UNIQUE INDEX person_alias_sheet_key ON person_alias (sheet_id, alias_key);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.person_alias AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_alias t WHERE FALSE;
CREATE INDEX hist_person_alias_snapshot ON hist.person_alias (snapshot_id);
CREATE TABLE asof.person_alias AS SELECT * FROM person_alias WHERE FALSE;
