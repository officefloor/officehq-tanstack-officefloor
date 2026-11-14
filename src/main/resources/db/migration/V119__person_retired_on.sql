-- The sheet's today when the person was retired (null: from the beginning of time), so a pool's cuts taken as they
-- stood at the end of an earlier period count the person retired only when the retirement was made on or before it.
ALTER TABLE person ADD COLUMN retired_on DATE;
