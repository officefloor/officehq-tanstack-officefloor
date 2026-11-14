-- The sheet's default grade (A, B or C; null: blank, A): the grade the quick-add bar gives a new person and an
-- import line with a blank grade takes. Not dated.
ALTER TABLE sheet ADD COLUMN default_grade VARCHAR(10);
