-- An invoice can now be cancelled when it was sent by mistake: the lifecycle gains a terminal VOID
-- state (DRAFT -> SENT -> PAID, or VOID once cancelled). A VOID invoice no longer counts towards what
-- is owed. The status column is a free VARCHAR(16) with no value constraint, so VOID needs no
-- structural change; this records the new state on the column so the schema documents the full
-- lifecycle. Additive and idempotent.
COMMENT ON COLUMN invoices.status IS
    'Invoice lifecycle: DRAFT -> SENT -> PAID, or VOID once cancelled (a mistaken send).';
