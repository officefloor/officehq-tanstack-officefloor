-- A person may be retired from a date (none while active). A claim dated after it cannot be added or
-- edited to; claims dated on or before it still can. A retired person's claims keep counting.
ALTER TABLE person ADD COLUMN retired_from DATE;
