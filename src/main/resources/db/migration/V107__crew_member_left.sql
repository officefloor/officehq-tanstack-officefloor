-- Crew memberships a merge removed, kept as they stood (under the member's name then) with the sheet's today they left,
-- so the crews read as of a date before the merge still show them. id is the membership's own id (its order in the crew).
CREATE TABLE crew_member_left (
    id        BIGINT       PRIMARY KEY,
    crew_id   BIGINT       NOT NULL REFERENCES crew (id),
    person_id BIGINT       NOT NULL,
    name      VARCHAR(500) NOT NULL,
    made_on   DATE,
    left_on   DATE         NOT NULL
);
