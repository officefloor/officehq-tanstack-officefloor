-- A charge line now records not just how many (qty) but the unit that quantity is measured in — the
-- "hours", "days", "items" a line is charged in — so the invoice can read "2 hours" rather than a
-- bare "2". Additive: a NOT NULL text column with a default so any existing line gets a sensible
-- unit, and the write paths (seed, POST /api/lineitems, POST /api/lineitems/edit) supply it going
-- forward. The line's amount is still qty * unit price; the unit is descriptive only.
ALTER TABLE line_item ADD COLUMN unit VARCHAR(50) NOT NULL DEFAULT 'unit';
