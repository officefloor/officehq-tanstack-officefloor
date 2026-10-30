-- The day a SNAPSHOT line was taken on: the sheet's today it moved to (set_on is the day being left).
-- Null for lines kept before it was recorded.
ALTER TABLE person_snapshot ADD COLUMN taken_on DATE;
