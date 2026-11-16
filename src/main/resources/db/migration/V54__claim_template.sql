-- The sheet's claim templates: each name (trimmed, not blank) at most once, kept in the order first saved. Saving a
-- name again replaces its amount and note; a claim made from a template copies them, so later saves do not alter it.
CREATE TABLE claim_template (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200)   NOT NULL,
    name     VARCHAR(200)   NOT NULL,
    amount   DECIMAL(17, 2) NOT NULL,
    note     VARCHAR(2000)  NOT NULL,
    CONSTRAINT claim_template_unique UNIQUE (sheet_id, name)
);
