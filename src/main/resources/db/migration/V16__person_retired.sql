-- The date a person is retired from (null: not retired). A claim dated after it is refused; the person's
-- claims keep counting in every total.
ALTER TABLE person ADD COLUMN retired_from DATE;
