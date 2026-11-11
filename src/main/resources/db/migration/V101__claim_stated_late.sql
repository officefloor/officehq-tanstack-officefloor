-- The late charge a statement bound the claim to, with its figure (null: none bound, as for a
-- binding kept before late charges were bound: the charge then follows the sheet's today).
ALTER TABLE claim ADD COLUMN stated_late DECIMAL(19, 2);
ALTER TABLE claim_history ADD COLUMN stated_late DECIMAL(19, 2);
ALTER TABLE claim_binding ADD COLUMN stated_late DECIMAL(19, 2);
