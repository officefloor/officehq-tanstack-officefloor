-- Who holds a claim's editor open: the signed-in name that opened it, so no one else can edit the claim until it is
-- saved or cancelled, or the sheet's today changes. Null while no editor holds it.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(255);
