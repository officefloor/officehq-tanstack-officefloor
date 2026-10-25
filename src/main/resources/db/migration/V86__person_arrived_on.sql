-- The sheet's today when a transfer brought the person here (null: they did not arrive by transfer).
ALTER TABLE person ADD COLUMN arrived_on DATE;
