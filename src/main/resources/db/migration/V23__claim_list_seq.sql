-- A claim's place in its sheet's list, newest first: given when added and again when it is moved to
-- another person (so a moved claim is listed as its new person's newest). Ties fall back to the id.
ALTER TABLE claim ADD COLUMN list_seq BIGINT DEFAULT 0 NOT NULL;
UPDATE claim SET list_seq = id;
