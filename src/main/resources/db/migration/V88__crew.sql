-- The sheet's crews: a name (trimmed, unique on the sheet) and a lead (a person of the sheet), listed in the order they
-- were added (id order).
CREATE TABLE crew (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    name VARCHAR(200) NOT NULL,
    lead_id BIGINT NOT NULL REFERENCES person (id),
    UNIQUE (sheet_id, name)
);
-- A crew's members: a person (at most once per crew, never its lead, in any number of crews), listed in the order they
-- were added (id order). Kept with the sheet's id, so it goes with the sheet's other rows.
CREATE TABLE crew_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    crew_id BIGINT NOT NULL REFERENCES crew (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    UNIQUE (crew_id, person_id)
);
