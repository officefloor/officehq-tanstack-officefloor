-- The sheet's crews: a name (stored trimmed, at most once on its sheet: the app refuses a repeat with a message, the
-- index is the backstop) and a lead, a person of the sheet; and its members, each a person of the sheet at most once in
-- the crew and never its lead.
CREATE TABLE crew (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name      VARCHAR(200) NOT NULL,
    lead_id   BIGINT       NOT NULL REFERENCES person (id)
);
CREATE UNIQUE INDEX crew_sheet_name ON crew (sheet_id, name);

CREATE TABLE crew_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    crew_id   BIGINT       NOT NULL REFERENCES crew (id),
    person_id BIGINT       NOT NULL REFERENCES person (id)
);
CREATE UNIQUE INDEX crew_member_crew_person ON crew_member (crew_id, person_id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.crew AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM crew t WHERE FALSE;
CREATE INDEX hist_crew_snapshot ON hist.crew (snapshot_id);
CREATE TABLE asof.crew AS SELECT * FROM crew WHERE FALSE;
CREATE TABLE hist.crew_member AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM crew_member t WHERE FALSE;
CREATE INDEX hist_crew_member_snapshot ON hist.crew_member (snapshot_id);
CREATE TABLE asof.crew_member AS SELECT * FROM crew_member WHERE FALSE;
