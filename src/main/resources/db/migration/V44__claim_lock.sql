-- Who holds the claim's editor open (null: no one). Taken at the edit button, released when the editor is saved or
-- cancelled, and released on every claim of the sheet when the sheet's today changes.
ALTER TABLE claim ADD COLUMN locked_by VARCHAR(255);
