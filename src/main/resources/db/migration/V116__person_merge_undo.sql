-- The last merge into a person, kept so the steward who made it can undo it on the same sheet-today: who made it
-- and on which today, the merged person as they were (their row, dated grades and terms), the names they brought
-- to the other's aliases (their name, then their aliases, in order) and the claims that came from them.
CREATE TABLE person_merge (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL REFERENCES sheet (id),
    into_id   BIGINT       NOT NULL,
    from_id   BIGINT       NOT NULL,
    merged_by VARCHAR(255) NOT NULL,
    merged_on DATE         NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id);

-- The merged person's row as it was, by merge.
CREATE TABLE person_merge_row AS SELECT * FROM person WITH NO DATA;
ALTER TABLE person_merge_row ADD COLUMN merge_id BIGINT;

CREATE TABLE person_merge_grade (
    merge_id  BIGINT     NOT NULL REFERENCES person_merge (id),
    grade     VARCHAR(3) NOT NULL,
    from_date DATE
);

CREATE TABLE person_merge_terms (
    merge_id  BIGINT NOT NULL REFERENCES person_merge (id),
    terms     INT,
    from_date DATE   NOT NULL
);

-- The merged person's own aliases (seq from 1, in order); their name is the alias at seq 0.
CREATE TABLE person_merge_alias (
    merge_id BIGINT       NOT NULL REFERENCES person_merge (id),
    seq      INT          NOT NULL,
    alias    VARCHAR(500) NOT NULL
);

CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL REFERENCES person_merge (id),
    claim_id BIGINT NOT NULL
);
