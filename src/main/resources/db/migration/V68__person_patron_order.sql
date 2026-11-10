-- The order a person's patrons were named in (null for those named before it was kept: before the
-- rest, by patron id). The first-named patron takes the remainder of the shares.
CREATE SEQUENCE person_patron_seq;
ALTER TABLE person_patron ADD COLUMN named_seq BIGINT;
