-- The sheet's today a transfer brought the person to this sheet on (NULL: they did not arrive by transfer).
ALTER TABLE person ADD COLUMN arrived_on DATE;
