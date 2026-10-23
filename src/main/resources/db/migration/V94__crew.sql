-- The sheet's crews: a name (trimmed) at most once on a sheet, and its lead (a person on the sheet).
CREATE TABLE crew (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    name      VARCHAR(200) NOT NULL,
    lead_id   BIGINT       NOT NULL,
    added_by  VARCHAR(100) NOT NULL,
    CONSTRAINT crew_name UNIQUE (sheet_id, name)
);

-- A crew's members: a person at most once in a crew (and never its lead).
CREATE TABLE crew_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    crew_id   BIGINT       NOT NULL REFERENCES crew(id),
    person_id BIGINT       NOT NULL,
    added_by  VARCHAR(100) NOT NULL,
    CONSTRAINT crew_member_person UNIQUE (crew_id, person_id)
);
