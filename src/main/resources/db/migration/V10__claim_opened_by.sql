-- Who opened a claim: set when it is opened, null while a draft. That user may not settle it (four eyes).
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(200);
