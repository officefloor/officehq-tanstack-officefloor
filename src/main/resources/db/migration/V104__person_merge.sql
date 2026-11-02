-- Each merge of a person into another, kept so the steward who made it can undo the last merge into a person on the
-- same sheet-today: who made it, on which sheet-today, the merged person as they were (kept under their own id), the
-- claims that came from them and their own aliases. An undone merge stays, marked undone.
CREATE TABLE person_merge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet       VARCHAR(200)  NOT NULL,
    from_id     BIGINT        NOT NULL,
    into_id     BIGINT        NOT NULL,
    made_by     VARCHAR(200)  NOT NULL,
    made_on     DATE          NOT NULL,
    undone      BOOLEAN       DEFAULT FALSE NOT NULL,
    name        VARCHAR(200)  NOT NULL,
    terms       INT,
    contact     VARCHAR(255),
    origin      VARCHAR(200),
    tags        VARCHAR(4000) DEFAULT '' NOT NULL,
    flag_reason VARCHAR(255),
    flag_until  DATE,
    approver    VARCHAR(200),
    arrived_on  DATE
);
CREATE INDEX person_merge_into ON person_merge (sheet, into_id, id);
CREATE TABLE person_merge_claim (
    merge_id BIGINT NOT NULL,
    claim    BIGINT NOT NULL,
    PRIMARY KEY (merge_id, claim)
);
CREATE TABLE person_merge_alias (
    merge_id BIGINT       NOT NULL,
    alias    VARCHAR(200) NOT NULL,
    PRIMARY KEY (merge_id, alias)
);
CREATE TABLE person_merge_grade (
    merge_id  BIGINT     NOT NULL,
    grade     VARCHAR(1) NOT NULL,
    from_date DATE
);
CREATE TABLE person_merge_terms (
    merge_id  BIGINT NOT NULL,
    terms     INT,
    from_date DATE   NOT NULL
);
