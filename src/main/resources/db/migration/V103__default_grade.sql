-- The sheet's default grade (not dated): the grade the quick-add bar gives a new person, and an import line with a
-- blank grade takes. Null: A.
ALTER TABLE sheet ADD COLUMN default_grade VARCHAR(1);
