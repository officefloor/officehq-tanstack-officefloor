-- The merges done on a sheet, kept so the last merge into a person can be undone on the sheet-today it was made: the
-- merged person as they were (their id, name, grades, terms, contact and tags), who merged them and on what day, and
-- which claims and aliases went with them to the person merged into. An undone merge is marked so.
CREATE TABLE person_merge (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id        VARCHAR(255) NOT NULL,
    into_id         BIGINT       NOT NULL,
    from_id         BIGINT       NOT NULL,
    name            VARCHAR(255) NOT NULL,
    grade           VARCHAR(1)   NOT NULL,
    terms           INT,
    contact         VARCHAR(255),
    tags            VARCHAR(4000),
    origin_sheet_id VARCHAR(255),
    origin_name     VARCHAR(255),
    transferred_on  DATE,
    merged_by       VARCHAR(255) NOT NULL,
    merged_on       DATE         NOT NULL,
    undone          BOOLEAN DEFAULT FALSE NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id, id);
CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL,
    claim_id BIGINT NOT NULL
);
CREATE INDEX person_merge_claim_merge ON person_merge_claim (merge_id);
-- The aliases that went with the merged person: their own, and the alias their name became.
CREATE TABLE person_merge_alias (
    merge_id BIGINT NOT NULL,
    alias_id BIGINT NOT NULL
);
CREATE INDEX person_merge_alias_merge ON person_merge_alias (merge_id);
CREATE TABLE person_merge_grade (
    merge_id  BIGINT     NOT NULL,
    from_date DATE       NOT NULL,
    grade     VARCHAR(1) NOT NULL
);
CREATE TABLE person_merge_terms (
    merge_id  BIGINT NOT NULL,
    from_date DATE   NOT NULL,
    terms     INT
);
