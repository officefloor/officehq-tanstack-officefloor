-- Each merge of a person into another, so the last one into a person can be undone on the sheet's
-- today it was made: the people, and the sheet's today then.
CREATE TABLE person_merge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    from_id BIGINT NOT NULL REFERENCES person(id),
    into_id BIGINT NOT NULL REFERENCES person(id),
    merged_on DATE
);
CREATE INDEX person_merge_into ON person_merge(into_id);
-- The claims a merge moved to the person merged into.
CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL REFERENCES person_merge(id),
    claim_id BIGINT NOT NULL
);
CREATE INDEX person_merge_claim_merge ON person_merge_claim(merge_id);
-- The merged person's own aliases as they stood before the merge, given back when it is undone.
CREATE TABLE person_merge_alias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merge_id BIGINT NOT NULL REFERENCES person_merge(id),
    alias VARCHAR(200) NOT NULL,
    added_on DATE,
    alias_merge_id BIGINT
);
CREATE INDEX person_merge_alias_merge ON person_merge_alias(merge_id);
-- The merge that gave the person the alias (null: given directly): undoing it takes the alias away.
ALTER TABLE person_alias ADD COLUMN merge_id BIGINT;
