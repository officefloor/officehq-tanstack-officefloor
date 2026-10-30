-- Claims that left the sheet with their person on a transfer, as they stood when they left (transferred_on: the
-- sheet's today then), so the as-of view of an earlier date still shows them. A column later added to claim must be
-- added here too, before transferred_on (claims are copied in with SELECT c.*).
CREATE TABLE claim_transferred AS SELECT * FROM claim WITH NO DATA;
ALTER TABLE claim_transferred ADD COLUMN transferred_on DATE;
