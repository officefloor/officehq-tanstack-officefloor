-- Every tag carried by a claim that has since left the sheet (with a person transferred away), sorted and comma
-- separated; null while none has. The tag totals still list these as tags used on the sheet.
ALTER TABLE sheet ADD COLUMN departed_tags VARCHAR(1024);
