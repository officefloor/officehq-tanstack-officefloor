-- Each merge of a person into another, so the last one into a person can be undone on the sheet-day it was made: who
-- was merged into whom, who made it, the sheet's today then, the alias the merged person's name became, how many
-- statements the person merged into had then, and whether it has been undone since.
CREATE TABLE person_merge (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id        VARCHAR(100) NOT NULL REFERENCES sheet(id),
    from_id         BIGINT       NOT NULL,
    from_name       VARCHAR(200) NOT NULL,
    into_id         BIGINT       NOT NULL,
    made_by         VARCHAR(100) NOT NULL,
    made_on         DATE,
    name_alias_id   BIGINT,
    into_statements INT          DEFAULT 0 NOT NULL,
    undone          BOOLEAN      DEFAULT FALSE NOT NULL
);
CREATE INDEX person_merge_into ON person_merge (sheet_id, into_id, id);

-- The claims a merge moved to the person merged into.
CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL REFERENCES person_merge(id),
    claim_id BIGINT NOT NULL
);

-- The aliases of the merged person a merge moved to the person merged into.
CREATE TABLE person_merge_alias (
    merge_id BIGINT NOT NULL REFERENCES person_merge(id),
    alias_id BIGINT NOT NULL
);

-- The merged person's own rows as they were before the merge (the person and their grades, terms, holds, flags,
-- approver, statements and retirement), column by column, so an undo can put them back.
CREATE TABLE person_merge_row (
    merge_id    BIGINT       NOT NULL REFERENCES person_merge(id),
    table_name  VARCHAR(100) NOT NULL,
    row_no      INT          NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    val         VARCHAR(4000)
);
CREATE INDEX person_merge_row_merge ON person_merge_row (merge_id, table_name, row_no);
