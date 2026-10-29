-- A plan may name one of its sheet's descendants: its claims are then generated for the person of the same name on that
-- sheet (none while there is no such person), by that sheet's rules. '' for a plan making claims on its own sheet.
ALTER TABLE plan ADD COLUMN target_sheet VARCHAR(255) DEFAULT '' NOT NULL;
