-- A sheet's default grade (A, B or C; null means A; not dated): the grade the quick-add bar gives a new person,
-- and the grade of an imported line that leaves its grade blank.
ALTER TABLE sheet ADD COLUMN default_grade VARCHAR(1);
