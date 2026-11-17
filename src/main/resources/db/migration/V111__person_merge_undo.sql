-- Every merge of a person into another, kept so the last merge into a person can be undone on the sheet's today it was
-- made, by the user who made it: who was merged (from) into whom, the alias their name became (name_alias_id), the claims
-- and aliases that came from them, and their own rows the merge removed (the merge_saved_* copies, by merge_id).
CREATE TABLE person_merge (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200) NOT NULL,
    from_id       BIGINT       NOT NULL,
    into_id       BIGINT       NOT NULL,
    by_user       VARCHAR(200) NOT NULL,
    made_on       DATE         NOT NULL,
    name_alias_id BIGINT,
    undone        BOOLEAN      DEFAULT FALSE NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id, id);

CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL REFERENCES person_merge (id),
    claim_id BIGINT NOT NULL
);
CREATE INDEX person_merge_claim_merge ON person_merge_claim (merge_id);

CREATE TABLE person_merge_alias (
    merge_id BIGINT NOT NULL REFERENCES person_merge (id),
    alias_id BIGINT NOT NULL
);
CREATE INDEX person_merge_alias_merge ON person_merge_alias (merge_id);

-- The merged person's own rows as they stood just before the merge removed them.
CREATE TABLE merge_saved_person AS SELECT * FROM person WHERE FALSE;
CREATE TABLE merge_saved_person_hold AS SELECT * FROM person_hold WHERE FALSE;
CREATE TABLE merge_saved_watch AS SELECT * FROM watch WHERE FALSE;
CREATE TABLE merge_saved_person_approver AS SELECT * FROM person_approver WHERE FALSE;
CREATE TABLE merge_saved_person_grade AS SELECT * FROM person_grade WHERE FALSE;
CREATE TABLE merge_saved_person_grade_log AS SELECT * FROM person_grade_log WHERE FALSE;
CREATE TABLE merge_saved_person_retire_log AS SELECT * FROM person_retire_log WHERE FALSE;
CREATE TABLE merge_saved_person_retirement AS SELECT * FROM person_retirement WHERE FALSE;
CREATE TABLE merge_saved_person_terms_log AS SELECT * FROM person_terms_log WHERE FALSE;
CREATE TABLE merge_saved_person_terms_from AS SELECT * FROM person_terms_from WHERE FALSE;
ALTER TABLE merge_saved_person ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_hold ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_watch ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_approver ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_grade ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_grade_log ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_retire_log ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_retirement ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_terms_log ADD COLUMN merge_id BIGINT;
ALTER TABLE merge_saved_person_terms_from ADD COLUMN merge_id BIGINT;
