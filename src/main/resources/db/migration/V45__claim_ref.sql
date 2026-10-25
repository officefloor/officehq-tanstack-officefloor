-- A claim may carry an external reference (null: none), used at most once on a sheet, comparing without regard to case.
ALTER TABLE claim ADD COLUMN ref VARCHAR(200);
ALTER TABLE hist.claim ADD COLUMN ref VARCHAR(200);
ALTER TABLE asof.claim ADD COLUMN ref VARCHAR(200);
