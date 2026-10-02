-- A client can pay one lump sum and have it split across several of their open invoices. Each share
-- is still its own payment row against one invoice (so every invoice's balance and status derive the
-- same one-place way from its payments), but the shares that came from the same lump payment carry a
-- shared reference so the split is recorded as one payment rather than coincidental rows. Nullable: a
-- payment recorded against a single invoice on its own has no batch.
ALTER TABLE payments ADD COLUMN batch_ref VARCHAR(64);
