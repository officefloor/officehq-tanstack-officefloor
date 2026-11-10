-- A sheet's dated settings: each value a figure setting takes from a date. The value in force on a
-- date is the one with the latest from-date on or before it; before every from-date the setting has
-- its starting value (floor 1.00, late percent 0.00, terms 14; none for the others). Keys:
-- clerk-limit, floor, terms, late-pct, terms-A, terms-B, terms-C, cap. A blank value is "none"
-- (no clerk limit, no cap, a grade on the sheet's terms).
CREATE TABLE setting_value (
    sheet_id VARCHAR(200) NOT NULL REFERENCES sheet(id),
    setting_key VARCHAR(40) NOT NULL,
    from_date DATE NOT NULL,
    setting_value VARCHAR(40) NOT NULL,
    PRIMARY KEY (sheet_id, setting_key, from_date)
);
-- The values already set carry over from the sheet's today.
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'floor', today, CAST(due_floor AS VARCHAR) FROM sheet WHERE due_floor <> 1.00;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'terms', today, CAST(terms AS VARCHAR) FROM sheet WHERE terms <> 14;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'late-pct', today, CAST(late_pct AS VARCHAR) FROM sheet WHERE late_pct <> 0.00;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'clerk-limit', today, CAST(clerk_limit AS VARCHAR) FROM sheet WHERE clerk_limit IS NOT NULL;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'cap', today, CAST(cap AS VARCHAR) FROM sheet WHERE cap IS NOT NULL;
INSERT INTO setting_value (sheet_id, setting_key, from_date, setting_value)
    SELECT g.sheet_id, 'terms-' || g.grade, s.today, CAST(g.terms AS VARCHAR) FROM grade_terms g JOIN sheet s ON s.id = g.sheet_id;
