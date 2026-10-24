-- A sheet's claim templates: a trimmed name (on the sheet at most once; saving it again replaces the template), an
-- amount the claim form accepts and a trimmed note. A claim made from one copies its amount and note at that moment.
CREATE TABLE claim_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet (id),
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    note VARCHAR(2000) NOT NULL DEFAULT ''
);
CREATE UNIQUE INDEX claim_template_sheet_name ON claim_template (sheet_id, name);
