-- Each merge of a person into another, so the last merge into a person can be undone on the sheet's today it was
-- made, by the user who made it: the merged person as they stood (their state, as entity_history keeps it), the ids
-- of the claims that moved, their own aliases and the ids of the aliases the merge gave the other (newline separated).
CREATE TABLE person_merge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    into_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    made_on DATE NOT NULL,
    made_by VARCHAR(255) NOT NULL,
    state CLOB NOT NULL,
    claim_ids CLOB NOT NULL,
    aliases CLOB NOT NULL,
    alias_ids CLOB NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (into_id, id);
