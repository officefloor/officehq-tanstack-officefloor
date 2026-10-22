-- The sheet's rounding policy date (null: the sheet's today): a claim dated before it rounds half up, one dated on or
-- after it half to even. A correction rounds by the date of the claim it corrects.
ALTER TABLE sheet ADD COLUMN policy_date DATE;
