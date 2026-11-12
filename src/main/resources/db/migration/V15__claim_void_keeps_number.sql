-- A voided claim now keeps the number it was given when opened (still never reused). Claims voided
-- before this change had theirs cleared and stay without one.
COMMENT ON COLUMN claim.number IS 'Given when opened, the next on its sheet; kept when voided, never reused';
