-- A sheet's crews: a trimmed name, on the sheet at most once (ignoring case), and a lead, a person of the sheet.
CREATE TABLE crew (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name VARCHAR(255) NOT NULL,
    lead_id BIGINT NOT NULL REFERENCES person (id)
);
CREATE UNIQUE INDEX crew_sheet_name ON crew (sheet_id, name);
-- A crew's members: people of the sheet, in the crew at most once, never its lead.
CREATE TABLE crew_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_id BIGINT NOT NULL REFERENCES crew (id),
    person_id BIGINT NOT NULL REFERENCES person (id)
);
CREATE UNIQUE INDEX crew_member_crew_person ON crew_member (crew_id, person_id);
