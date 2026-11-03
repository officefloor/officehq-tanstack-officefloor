-- The sheet-today a transferred person arrived on this sheet (null for anyone not brought here by a transfer).
ALTER TABLE person ADD COLUMN arrived_on DATE;
