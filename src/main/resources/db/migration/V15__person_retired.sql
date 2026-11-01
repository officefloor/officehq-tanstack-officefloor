-- The date a person is retired from; null while they are not retired. Set at all, they count as retired.
ALTER TABLE person ADD COLUMN retired_from DATE;
