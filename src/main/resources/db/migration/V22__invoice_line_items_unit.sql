-- Charge lines now carry the UNIT the quantity is measured in (e.g. "hours", "items", "days"), so a
-- line reads "2 hours" not a bare "2". Additive: a new NOT NULL column with a default so any existing
-- rows remain valid; new lines supply their own unit through the API. A line's amount is still
-- qty * unit_price — the unit is descriptive, not part of the arithmetic.
ALTER TABLE invoice_line_items ADD COLUMN unit VARCHAR(50) NOT NULL DEFAULT '';
