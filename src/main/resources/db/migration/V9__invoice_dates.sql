-- Invoices gain two dates: the day the invoice was issued (went out) and the day it is due. Stored
-- as ISO date literals (YYYY-MM-DD) so they round-trip to the UI unchanged. Nullable and additive,
-- so existing rows and the create path (which does not set them) need no change.
ALTER TABLE invoice ADD COLUMN issued_date VARCHAR(10);
ALTER TABLE invoice ADD COLUMN due_date VARCHAR(10);
