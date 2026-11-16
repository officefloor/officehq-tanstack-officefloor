-- Who holds a claim's editor open (the user who pressed its edit button), so no one else can open it until it is
-- saved or cancelled or the sheet's today changes. Null when no one does.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(200);
