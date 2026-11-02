-- The descendant sheet a plan generates its claims on, for the person of the same name there (null: its own sheet).
ALTER TABLE plan ADD COLUMN target_sheet VARCHAR(200);
