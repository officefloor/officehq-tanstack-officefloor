-- Each merge of a person into another, kept so the last merge into a person can be undone on the sheet-today it
-- was made by the steward who made it: the merged person's own row (name, terms, contact, tags, where they came
-- from, version), their grades and dated terms, the alias rows that moved with them (their name's alias among
-- them) and the claims that came from them. An undone merge is marked so.
CREATE TABLE person_merge (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(255) NOT NULL,
    from_id       BIGINT       NOT NULL,
    into_id       BIGINT       NOT NULL,
    merged_by     VARCHAR(255) NOT NULL,
    merged_on     DATE,
    undone        BOOLEAN      DEFAULT FALSE NOT NULL,
    name          VARCHAR(255) NOT NULL,
    terms         INT,
    contact       VARCHAR(255),
    tags          VARCHAR(4000),
    origin        VARCHAR(255),
    arrived_on    DATE,
    version       BIGINT       NOT NULL,
    name_alias_id BIGINT
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id, id);
CREATE TABLE person_merge_grade (
    merge_id  BIGINT      NOT NULL,
    from_date DATE        NOT NULL,
    grade     VARCHAR(255) NOT NULL
);
CREATE TABLE person_merge_terms (
    merge_id  BIGINT NOT NULL,
    from_date DATE   NOT NULL,
    terms     INT
);
CREATE TABLE person_merge_alias (
    merge_id BIGINT NOT NULL,
    alias_id BIGINT NOT NULL
);
CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL,
    claim_id BIGINT NOT NULL
);
