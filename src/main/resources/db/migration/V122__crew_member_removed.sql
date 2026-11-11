-- Crew memberships a merge took away: the member row as it stood (its id keeps its place in the crew's
-- order) and the sheet's today when it was taken away, so the crews can still be read as of an earlier date.
CREATE TABLE crew_member_removed (
    id BIGINT NOT NULL,
    crew_id BIGINT NOT NULL REFERENCES crew(id),
    person_id BIGINT NOT NULL REFERENCES person(id),
    added_on DATE,
    removed_on DATE
);
