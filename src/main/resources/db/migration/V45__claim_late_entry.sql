-- Whether the claim was added while its period was soft-closed (a late entry). Fixed when it is added.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN NOT NULL DEFAULT FALSE;
-- A soft-closed period: its due-total and each person's are fixed (as a close does), but its claims can still be
-- changed. Closed and soft are never both set.
ALTER TABLE period ADD COLUMN soft BOOLEAN NOT NULL DEFAULT FALSE;
