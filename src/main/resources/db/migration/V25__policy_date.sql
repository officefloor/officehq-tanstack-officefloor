-- The sheet's policy date: a claim dated before it rounds its live figures half up, one dated on or after it
-- half to even. Null (blank) means the sheet's today.
ALTER TABLE sheet ADD COLUMN policy_date DATE;
-- The date of the claim a correction corrects: a correction rounds by that date, not its own. Null for any other
-- claim. (The corrected claim is SETTLED, so its date never changes.)
ALTER TABLE claim ADD COLUMN corrects_date DATE;
