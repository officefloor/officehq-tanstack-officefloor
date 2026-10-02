-- A charge line now records the unit it is measured in (e.g. "hours", "days", "items") alongside how
-- many (qty) and the price of each (unit_price), so a line reads "2 hours" rather than a bare "2".
-- New lines default to "units"; existing rows adopt the same default via the column default.
ALTER TABLE invoice_line_items ADD COLUMN unit VARCHAR(50) NOT NULL DEFAULT 'units';
