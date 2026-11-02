-- The sheet's crews, each name (trimmed) unique on it, in the order added, each led by a person on the sheet, and their
-- members: people on the sheet, each at most once in a crew and never its lead.
CREATE TABLE crew (
    sheet     VARCHAR(200) NOT NULL,
    name      VARCHAR(200) NOT NULL,
    lead_id   BIGINT       NOT NULL,
    position  INT          NOT NULL,
    PRIMARY KEY (sheet, name)
);
CREATE TABLE crew_member (
    sheet     VARCHAR(200) NOT NULL,
    crew      VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    position  INT          NOT NULL,
    PRIMARY KEY (sheet, crew, person_id)
);
