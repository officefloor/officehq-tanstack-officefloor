-- Whether the claim is held (toggled while OPEN or PENDING): while held and not bound by a statement, it counts in no
-- live total, as the claims of a held person do. The flag stays as it is once the claim moves on.
ALTER TABLE claim ADD COLUMN held BOOLEAN NOT NULL DEFAULT FALSE;
