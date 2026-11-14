-- A merge made through the row, so the steward who made it can undo it on the same sheet-today: who was merged into
-- whom, by whom and on which today (undone once undone). The merged person's own rows (the person, their grades,
-- terms, holds, past retirements, watches and aliases) are kept as they stood, under the merge, and the claims that
-- came from them with the place they were listed at.
CREATE TABLE person_merge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(100) NOT NULL,
    person_id BIGINT NOT NULL,
    into_id BIGINT NOT NULL,
    by_user VARCHAR(100) NOT NULL,
    made_on DATE NOT NULL,
    undone BOOLEAN DEFAULT FALSE NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id);
CREATE TABLE person_merge_claim (
    sheet_id VARCHAR(100) NOT NULL,
    merge_id BIGINT NOT NULL,
    claim_id BIGINT NOT NULL,
    listed BIGINT
);
CREATE TABLE merged_person AS SELECT * FROM person WITH NO DATA;
ALTER TABLE merged_person ADD COLUMN merge_id BIGINT;
CREATE TABLE merged_person_grade AS SELECT * FROM person_grade WITH NO DATA;
ALTER TABLE merged_person_grade ADD COLUMN merge_id BIGINT;
CREATE TABLE merged_person_terms AS SELECT * FROM person_terms WITH NO DATA;
ALTER TABLE merged_person_terms ADD COLUMN merge_id BIGINT;
CREATE TABLE merged_person_hold AS SELECT * FROM person_hold WITH NO DATA;
ALTER TABLE merged_person_hold ADD COLUMN merge_id BIGINT;
CREATE TABLE merged_person_retirement AS SELECT * FROM person_retirement WITH NO DATA;
ALTER TABLE merged_person_retirement ADD COLUMN merge_id BIGINT;
CREATE TABLE merged_person_watch AS SELECT * FROM person_watch WITH NO DATA;
ALTER TABLE merged_person_watch ADD COLUMN merge_id BIGINT;
CREATE TABLE merged_person_alias AS SELECT * FROM person_alias WITH NO DATA;
ALTER TABLE merged_person_alias ADD COLUMN merge_id BIGINT;
