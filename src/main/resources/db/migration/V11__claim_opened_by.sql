-- Who opened a claim: the signed-in user who moved it from DRAFT to OPEN. Null until it is opened.
ALTER TABLE claim ADD COLUMN opened_by VARCHAR(255);
