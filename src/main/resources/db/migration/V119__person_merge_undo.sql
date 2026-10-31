-- Each merge of one person into another, so the last merge into a person can be undone (by whoever made it, on the
-- sheet's today it was made). alias_seq: the last person_alias seq before the merge added the merged person's names.
CREATE TABLE person_merge (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    from_id   BIGINT       NOT NULL,
    into_id   BIGINT       NOT NULL,
    merged_by VARCHAR(200),
    merged_on DATE,
    alias_seq BIGINT       NOT NULL,
    undone    BOOLEAN      DEFAULT FALSE NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id);
-- The claims a merge moved, with where they were listed before it.
CREATE TABLE person_merge_claim (
    merge_seq  BIGINT NOT NULL,
    claim_id   BIGINT NOT NULL,
    listed_seq BIGINT
);
-- What a merge dropped with the merged person, kept so an undo brings it back (keyed by the merged person's id).
CREATE TABLE person_alias_merged AS SELECT * FROM person_alias WITH NO DATA;
CREATE TABLE person_grade_merged AS SELECT * FROM person_grade WITH NO DATA;
CREATE TABLE person_terms_merged AS SELECT * FROM person_terms WITH NO DATA;
CREATE TABLE person_hold_merged AS SELECT * FROM person_hold WITH NO DATA;
CREATE TABLE person_retirement_merged AS SELECT * FROM person_retirement WITH NO DATA;
CREATE TABLE person_snapshot_merged AS SELECT * FROM person_snapshot WITH NO DATA;
CREATE TABLE person_watch_merged AS SELECT * FROM person_watch WITH NO DATA;
CREATE TABLE person_statement_merged AS SELECT * FROM person_statement WITH NO DATA;
