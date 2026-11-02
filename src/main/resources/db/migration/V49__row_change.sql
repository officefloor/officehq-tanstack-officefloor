-- Every change to a row of a sheet, for showing the sheet as of a past date (see history/AsOfView): each
-- insert, update or delete of a row of a sheet's tables, kept in the order made (id) with the sheet's
-- today when it was made (on_date) and the row's values before and after (each a serialized Object[] in
-- the table's column order; old_row null for an insert, new_row null for a delete). The rows are
-- captured by a trigger on each table (history/RowCapture), installed at start-up.
CREATE TABLE row_change (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id   VARCHAR(200) NOT NULL,
    on_date    DATE         NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    old_row    VARBINARY(1000000),
    new_row    VARBINARY(1000000)
);
CREATE INDEX row_change_sheet ON row_change (sheet_id, id);

-- The tables a sheet's view is replayed into: one per captured table, created at start-up.
CREATE SCHEMA IF NOT EXISTS asof;
