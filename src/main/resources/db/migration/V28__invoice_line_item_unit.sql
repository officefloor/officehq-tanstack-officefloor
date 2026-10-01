-- A charge line now records not just how many, but the UNIT that "how many" is counted in (hours,
-- days, items, ...). This is descriptive text that rides alongside the quantity; the line's amount is
-- still quantity times unit price regardless of the unit's name. Existing lines predate the unit, so
-- default them to a neutral "units"; the column is NOT NULL so every line always names its unit.
ALTER TABLE invoice_line_items ADD COLUMN unit VARCHAR(50) NOT NULL DEFAULT 'units';
