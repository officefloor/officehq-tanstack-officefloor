-- The sheet's crews, by name, in the order added; each has a lead, a person of the sheet.
CREATE TABLE crew (
    seq            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id       VARCHAR(200)  NOT NULL,
    name           VARCHAR(2000) NOT NULL,
    lead_person_id BIGINT        NOT NULL,
    UNIQUE (sheet_id, name)
);

-- A crew's members, in the order added: a person of the sheet, never the crew's lead, in a crew at most once.
CREATE TABLE crew_member (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    crew_name VARCHAR(2000) NOT NULL,
    person_id BIGINT        NOT NULL,
    UNIQUE (sheet_id, crew_name, person_id)
);
