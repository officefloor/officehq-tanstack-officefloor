-- A sheet's crews: a name (unique on the sheet, trimmed, not blank) and a lead, one of the sheet's people.
CREATE TABLE crew (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name      VARCHAR(500) NOT NULL,
    lead_id   BIGINT       NOT NULL,
    UNIQUE (sheet_id, name)
);
-- A crew's members: people of the sheet (at most once per crew, never its lead), listed in the order they were added (by id).
CREATE TABLE crew_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    crew_name VARCHAR(500) NOT NULL,
    person_id BIGINT       NOT NULL,
    UNIQUE (sheet_id, crew_name, person_id)
);
