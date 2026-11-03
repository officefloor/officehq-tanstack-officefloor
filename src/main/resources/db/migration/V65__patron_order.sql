-- The order a person named their patrons in (the first-named takes a share's remainder).
CREATE SEQUENCE patron_seq;
ALTER TABLE patron ADD COLUMN seq BIGINT DEFAULT NEXT VALUE FOR patron_seq NOT NULL;
