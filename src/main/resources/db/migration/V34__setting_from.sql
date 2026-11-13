-- A dated setting's values, each in force from its date until the next one's; before every from-date the
-- sheet's starting value (floor 1.00, late percent 0.00, terms 14; none for the clerk limit, the cap and a
-- grade's own terms). setting_value null: blank (no limit, no cap, a grade without its own terms).
CREATE TABLE setting_from (
    sheet_id VARCHAR(100) NOT NULL REFERENCES sheet (id),
    setting_key VARCHAR(20) NOT NULL,
    from_date DATE NOT NULL,
    setting_value VARCHAR(30),
    PRIMARY KEY (sheet_id, setting_key, from_date)
);
-- Values already set carry over as in force from the start.
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'floor', DATE '1900-01-01', CAST(due_floor AS VARCHAR(30)) FROM sheet WHERE due_floor <> 1.00;
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'late-pct', DATE '1900-01-01', CAST(late_pct AS VARCHAR(30)) FROM sheet WHERE late_pct <> 0.00;
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'terms', DATE '1900-01-01', CAST(terms AS VARCHAR(30)) FROM sheet WHERE terms <> 14;
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'clerk-limit', DATE '1900-01-01', CAST(clerk_limit AS VARCHAR(30)) FROM sheet WHERE clerk_limit IS NOT NULL;
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT id, 'cap', DATE '1900-01-01', CAST(due_cap AS VARCHAR(30)) FROM sheet WHERE due_cap IS NOT NULL;
INSERT INTO setting_from (sheet_id, setting_key, from_date, setting_value)
    SELECT sheet_id, 'terms-' || grade, DATE '1900-01-01', CAST(terms AS VARCHAR(30)) FROM grade_terms;
