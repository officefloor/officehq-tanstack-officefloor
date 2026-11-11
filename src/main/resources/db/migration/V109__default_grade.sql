-- The sheet's default grade, the grade a quick-added person and an imported line with a blank grade
-- take (null: A). Not dated.
ALTER TABLE sheet ADD COLUMN default_grade VARCHAR(10);
