-- A sheet's crews: a trimmed name, at most once per sheet, and a lead, a person on the sheet. A crew's
-- members are people on the sheet other than its lead, each at most once per crew. added_on is the
-- sheet's today when it was added, so the crews can be read as of a date.
CREATE TABLE crew (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    name VARCHAR(200) NOT NULL,
    lead_id BIGINT NOT NULL REFERENCES person(id),
    added_on DATE,
    UNIQUE (sheet_id, name)
);
CREATE TABLE crew_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_id BIGINT NOT NULL REFERENCES crew(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    added_on DATE,
    UNIQUE (crew_id, person_id)
);
