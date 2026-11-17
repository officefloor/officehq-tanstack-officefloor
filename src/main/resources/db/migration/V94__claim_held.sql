-- An OPEN or PENDING claim can be held and released: while held and not bound by a statement it counts in no live
-- total, as a claim dated within a hold of its person does. False: not held.
ALTER TABLE claim ADD COLUMN held BOOLEAN DEFAULT FALSE NOT NULL;
