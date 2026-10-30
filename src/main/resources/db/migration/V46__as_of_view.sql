-- History kept so the whole screen can be shown as of a past date (see V45): what V45 left out is recorded here,
-- each change dated the sheet's today when it was made (set_on). A NULL set_on is a value carried over from before
-- this history was kept: it counts as of any date.

-- The rest of what a claim's row shows, after each change to it.
ALTER TABLE claim_history ADD COLUMN claim_number VARCHAR(220);
ALTER TABLE claim_history ADD COLUMN opened_by    VARCHAR(255);
ALTER TABLE claim_history ADD COLUMN note         VARCHAR(2000);
ALTER TABLE claim_history ADD COLUMN proofs       INT;
ALTER TABLE claim_history ADD COLUMN split_from   VARCHAR(64);
ALTER TABLE claim_history ADD COLUMN late_entry   BOOLEAN;
UPDATE claim_history h SET (claim_number, opened_by, note, proofs, split_from, late_entry) =
    (SELECT c.claim_number, c.opened_by, c.note, c.proofs, c.split_from, c.late_entry FROM claim c
        WHERE c.sheet_id = h.sheet_id AND c.id = h.claim_id);

-- Each name and own terms recorded for a person, in the order recorded.
CREATE TABLE person_history (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200)  NOT NULL,
    person_id BIGINT        NOT NULL,
    set_on    DATE,
    name      VARCHAR(2000) NOT NULL,
    terms     INT
);
INSERT INTO person_history (sheet_id, person_id, set_on, name, terms) SELECT sheet_id, id, NULL, name, terms FROM person;

-- The sheet's today when each statement was taken, and when it was cancelled (a cancelled one is kept, marked).
ALTER TABLE person_statement ADD COLUMN set_on DATE;
ALTER TABLE person_statement ADD COLUMN cancelled BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE person_statement ADD COLUMN cancelled_on DATE;

-- The sheet's today when each patron was named.
ALTER TABLE person_patron ADD COLUMN set_on DATE;

-- Each start and stop of a watch, in the order made.
CREATE TABLE watch_history (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    set_on    DATE,
    watching  BOOLEAN      NOT NULL
);
INSERT INTO watch_history (sheet_id, person_id, user_name, set_on, watching)
    SELECT sheet_id, person_id, user_name, NULL, TRUE FROM person_watch ORDER BY seq;

-- The sheet's today when each period was added, and each close, soft-close and reopen, in the order made, with the
-- figures it fixed (person_totals: "<person id>=<total>" joined by ";").
ALTER TABLE period ADD COLUMN added_on DATE;
CREATE TABLE period_history (
    seq           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200)  NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    set_on        DATE,
    closed        BOOLEAN       NOT NULL,
    soft_closed   BOOLEAN       NOT NULL,
    closed_total  DECIMAL(14,2),
    person_totals VARCHAR(20000)
);

-- The sheet's today when it was archived.
ALTER TABLE sheet ADD COLUMN archived_on DATE;
INSERT INTO period_history (sheet_id, name, set_on, closed, soft_closed, closed_total, person_totals)
    SELECT p.sheet_id, p.name, NULL, p.closed, p.soft_closed, p.closed_total,
        (SELECT LISTAGG(t.person_id || '=' || t.total, ';') FROM period_person_total t
            WHERE t.sheet_id = p.sheet_id AND t.period_name = p.name)
    FROM period p WHERE p.closed OR p.soft_closed;

-- Each undated setting set (period numbering from, grace days, an action's permission, a user's role), in the order
-- set: its key ("numbering", "grace", "perm-<action>", "role-<user>") and value as text ("" for none).
CREATE TABLE plain_setting_history (
    seq           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      VARCHAR(200)  NOT NULL,
    set_on        DATE,
    setting_key   VARCHAR(400)  NOT NULL,
    setting_value VARCHAR(400)  NOT NULL
);

-- The sheet's today when each delegation was given.
ALTER TABLE sheet_delegation ADD COLUMN set_on DATE;
