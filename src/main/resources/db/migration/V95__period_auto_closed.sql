-- Whether the closed period was closed automatically (its grace days ran out) rather than by hand. A new claim is
-- refused only for a period closed by hand; a close by hand, or a reopen, clears it.
ALTER TABLE period ADD COLUMN auto_closed BOOLEAN NOT NULL DEFAULT FALSE;
