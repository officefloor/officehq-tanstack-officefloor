-- The order a person's patrons were named in: each row's place in one sheet-wide sequence (existing rows take it in
-- the order they are read). A person's patrons share 100.00 by it (see Person.getPatronShares).
CREATE SEQUENCE person_patron_named;
ALTER TABLE person_patron ADD COLUMN named BIGINT DEFAULT NEXT VALUE FOR person_patron_named NOT NULL;
-- The sheet's snapshots, and their layout as of a date, carry it too.
ALTER TABLE hist.person_patron ADD COLUMN named BIGINT;
ALTER TABLE asof.person_patron ADD COLUMN named BIGINT;
