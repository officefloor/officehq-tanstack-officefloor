-- The sheet's today when the claim last went to PENDING (null: never pending, or not recorded).
ALTER TABLE claim ADD COLUMN pending_on DATE;
