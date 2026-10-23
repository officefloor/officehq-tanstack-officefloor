-- The order periods were added in: each new period takes the next number from the sequence. Periods already
-- stored are numbered as they are found (ties with from_date are broken by from_date wherever the order is read).
CREATE SEQUENCE period_added_seq START WITH 1;
ALTER TABLE period ADD COLUMN added_seq BIGINT DEFAULT NEXT VALUE FOR period_added_seq;
UPDATE period SET added_seq = NEXT VALUE FOR period_added_seq WHERE added_seq IS NULL;
