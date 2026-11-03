-- Who last edited a claim: the signed-in user whose save (the claim form or the row's amount entry) last changed it. Null until edited.
ALTER TABLE claim ADD COLUMN edited_by VARCHAR(255);
