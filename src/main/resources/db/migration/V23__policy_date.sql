-- The sheet's policy date: a claim dated before it rounds its live figures half up, one dated on or
-- after it half to even. NULL: the sheet's today.
ALTER TABLE sheet ADD COLUMN policy_date DATE;
