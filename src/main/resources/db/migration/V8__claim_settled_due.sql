-- A settled claim's due: amount x its person's grade rate at the moment it was settled, kept as it
-- was so later rate changes do not alter it. Null for a claim not settled, or settled while its
-- grade had no rate (its due then stays live).
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(19, 2);
