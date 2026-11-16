-- A steward may waive an OPEN or PENDING claim's late charge: from the sheet's today it was waived on, the claim bears
-- no late charge. Null: never waived.
ALTER TABLE claim ADD COLUMN waived_on DATE;
