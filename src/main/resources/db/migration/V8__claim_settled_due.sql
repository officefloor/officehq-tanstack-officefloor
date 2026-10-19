-- A claim's due fixed when it was settled: amount x the person's grade's rate at that moment,
-- rounded half up to two decimals. NULL while the claim is not settled, or when it was settled
-- without a rate, so its due stays live.
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(12, 2);
