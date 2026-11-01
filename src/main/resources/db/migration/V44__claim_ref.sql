-- A claim's external reference (null for none): no two claims on a sheet share one, comparing without regard to case.
ALTER TABLE claim ADD COLUMN ref VARCHAR(200);
