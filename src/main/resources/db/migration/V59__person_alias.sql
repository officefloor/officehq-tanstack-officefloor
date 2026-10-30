-- A person's aliases: other names they go by, trimmed as typed, in the order added (seq), dated the
-- sheet's today when added. No alias equals, compared without regard to case, any name or alias on the sheet.
CREATE TABLE person_alias (
    seq       BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id  VARCHAR(200) NOT NULL,
    person_id BIGINT       NOT NULL,
    alias     VARCHAR(200) NOT NULL,
    set_on    DATE
);
