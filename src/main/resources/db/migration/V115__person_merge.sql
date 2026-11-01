-- Each merge of a person into another, so the last one into a person can be undone by the steward who
-- made it on the sheet-today it was made (`merged_on`). The merged person is not deleted: their row
-- (and their holds, watchers, grades, snapshots and statements) is parked on the sheet id
-- `~merged-<from_id>`, which no screen loads, until the merge is undone. `name_alias_id` is the alias
-- the merge gave into for from's name; person_merge_claim and person_merge_alias are the claims and
-- aliases that moved with them. Not sheet data of its own: no asof copies.
CREATE TABLE person_merge (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200) NOT NULL,
    from_id       BIGINT       NOT NULL,
    into_id       BIGINT       NOT NULL,
    merged_by     VARCHAR(200),
    merged_on     DATE         NOT NULL,
    name_alias_id BIGINT,
    undone        BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id, id);
CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL,
    claim_id BIGINT NOT NULL
);
CREATE INDEX person_merge_claim_merge ON person_merge_claim (merge_id);
CREATE TABLE person_merge_alias (
    merge_id BIGINT NOT NULL,
    alias_id BIGINT NOT NULL
);
CREATE INDEX person_merge_alias_merge ON person_merge_alias (merge_id);
