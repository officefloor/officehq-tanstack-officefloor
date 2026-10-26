-- A retired person can be unretired: their retirement is then kept as a past one, from its from-date to the day they
-- were unretired, and they are no longer retired (retired_from cleared). No claim dated strictly between a past
-- retirement's from-date and to-date can be added (or edited to such a date).
CREATE TABLE person_retirement (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES person (id),
    from_date DATE   NOT NULL,
    to_date   DATE   NOT NULL
);
CREATE INDEX person_retirement_person ON person_retirement (person_id, id);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.person_retirement AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_retirement t WHERE FALSE;
CREATE INDEX hist_person_retirement_snapshot ON hist.person_retirement (snapshot_id);
CREATE TABLE asof.person_retirement AS SELECT * FROM person_retirement WHERE FALSE;
