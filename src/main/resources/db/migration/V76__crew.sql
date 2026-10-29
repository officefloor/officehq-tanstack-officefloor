-- The sheet's crews: a name (trimmed, at most once per sheet) and a lead, a person of the sheet; listed in the order added.
CREATE TABLE crew (
    sheet_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    lead_id BIGINT NOT NULL,
    listed BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, name)
);
-- A crew's members: people of the sheet, at most once per crew and never its lead, in the order added. A person can be in
-- several crews.
CREATE TABLE crew_member (
    sheet_id VARCHAR(255) NOT NULL,
    crew_name VARCHAR(255) NOT NULL,
    person_id BIGINT NOT NULL,
    listed BIGINT NOT NULL,
    PRIMARY KEY (sheet_id, crew_name, person_id)
);
