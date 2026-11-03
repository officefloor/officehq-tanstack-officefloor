-- A claim's external reference (null: none). A reference is used at most once on a sheet, comparing
-- without regard to case (checked by the app).
ALTER TABLE claim ADD COLUMN ref VARCHAR(600);
