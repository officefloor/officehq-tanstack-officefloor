-- The sheet's today when the person last arrived on this sheet by a transfer (null: never transferred here); a steward
-- may undo the transfer on that day only.
ALTER TABLE person ADD COLUMN arrived_on DATE;
