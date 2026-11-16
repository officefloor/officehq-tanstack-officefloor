-- The sheet's crews: each name (trimmed, not blank) at most once on the sheet, kept in the order added, with a lead
-- chosen from the sheet's people.
CREATE TABLE crew (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    name     VARCHAR(500) NOT NULL,
    lead_id  BIGINT       NOT NULL REFERENCES person (id),
    CONSTRAINT crew_unique UNIQUE (sheet_id, name)
);

-- A crew's members: a person at most once in a crew, never its lead. A person can be in several crews.
CREATE TABLE crew_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_id   BIGINT NOT NULL REFERENCES crew (id),
    person_id BIGINT NOT NULL REFERENCES person (id),
    CONSTRAINT crew_member_unique UNIQUE (crew_id, person_id)
);
