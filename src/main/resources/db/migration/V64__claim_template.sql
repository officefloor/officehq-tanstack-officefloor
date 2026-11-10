-- A sheet's claim templates: a trimmed name (at most once per sheet; saving it again replaces it),
-- an amount the claim form would take and a note. A claim made from one copies its amount and note.
CREATE TABLE claim_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    name VARCHAR(200) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    UNIQUE (sheet_id, name)
);
