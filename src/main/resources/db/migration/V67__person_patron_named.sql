-- The order a person's patrons were named in: each new patron row takes the next number. Patron shares follow it.
CREATE SEQUENCE person_patron_named_seq;
ALTER TABLE person_patron ADD COLUMN named BIGINT DEFAULT NEXT VALUE FOR person_patron_named_seq NOT NULL;
