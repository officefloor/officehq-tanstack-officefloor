-- The sheet's default grade (setting key default-grade, one of the grades, not dated; blank: A): the quick-add bar
-- gives a new person it, and an import line with a blank grade uses it. Changing it changes no one.
ALTER TABLE sheet ADD COLUMN default_grade VARCHAR(1);
