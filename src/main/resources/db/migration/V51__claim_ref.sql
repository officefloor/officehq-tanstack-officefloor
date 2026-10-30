-- A claim's external reference, as given (surrounding spaces removed). Null: no reference.
-- Unique on the sheet comparing without regard to case (checked when a claim is added or edited).
ALTER TABLE claim ADD COLUMN ref VARCHAR(2000);
