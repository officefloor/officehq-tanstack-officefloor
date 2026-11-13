-- Settling a claim fixes its due (its amount at its person's grade's rate at that moment), so later
-- rate changes do not alter it. Null while the claim is live: not settled, or settled with no rate.
ALTER TABLE claim ADD COLUMN due DECIMAL(25, 2);
