-- A sheet a steward archived: one row per archived sheet, who archived it. An archived sheet stays visible and
-- refuses every change. A sheet with no row here is not archived.
CREATE TABLE sheet_archived (
    sheet_id    VARCHAR(200) PRIMARY KEY,
    archived_by VARCHAR(200) NOT NULL
);
