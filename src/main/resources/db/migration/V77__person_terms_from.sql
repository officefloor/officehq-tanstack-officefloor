-- A person's own terms may be dated: each row gives their terms (null: none) from its from-date, at most one per
-- from-date (a save for a from-date already recorded replaces it). On a date, the terms in force are those of the row
-- with the latest from-date on or before it; before every from-date, person.terms (from the beginning of time).
CREATE TABLE person_terms (
    person_id BIGINT NOT NULL REFERENCES person (id),
    terms     INT,
    from_date DATE   NOT NULL
);
CREATE UNIQUE INDEX person_terms_person ON person_terms (person_id, from_date);

-- Snapshotted with the rest of the screen (see V40, V41).
CREATE TABLE hist.person_terms AS SELECT t.*, CAST(NULL AS BIGINT) AS snapshot_id FROM person_terms t WHERE FALSE;
CREATE INDEX hist_person_terms_snapshot ON hist.person_terms (snapshot_id);
CREATE TABLE asof.person_terms AS SELECT * FROM person_terms WHERE FALSE;
