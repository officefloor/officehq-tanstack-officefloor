-- A statement now binds a claim's due as shown, late charge (and floor) included: stated_due is that due, and
-- while bound the claim shows it, its late charge no longer following the sheet's today.
COMMENT ON COLUMN claim.stated_due IS 'The due a statement bound, as shown then (late charge included); null when n/a';
