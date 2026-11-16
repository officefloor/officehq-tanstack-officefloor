-- The sheet's retired grades: a retired grade cannot be chosen for a new person or in an edit; people
-- already on it keep it.
CREATE TABLE grade_retired (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id    VARCHAR(200) NOT NULL REFERENCES sheet (id),
    grade       VARCHAR(10)  NOT NULL,
    retired_by  VARCHAR(200) NOT NULL,
    retired_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT grade_retired_unique UNIQUE (sheet_id, grade)
);
