-- Who opened a claim: set when it is opened, null while it is DRAFT. Its opener may not settle it.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
