-- A claim's due fixed when it is settled: its amount at the person's grade's rate at that moment,
-- kept as it was whatever the rates do later. Null while the claim is live (not settled, or settled
-- when the grade had no rate) — its due is then worked from the current rate.
ALTER TABLE claim ADD COLUMN settled_due DECIMAL(14,2);
