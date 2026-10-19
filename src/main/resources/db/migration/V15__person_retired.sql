-- retired_from: the date a person is retired from (NULL: not retired). No claim of theirs may be
-- dated after it; claims dated on or before it may still be added. Their claims keep counting in
-- every total.
ALTER TABLE person ADD COLUMN retired_from DATE;
