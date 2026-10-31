-- The order periods were added in: each new period takes the next number. Periods already on a sheet are
-- numbered by from-date, the order they were listed in.
CREATE SEQUENCE period_added_seq;
ALTER TABLE period ADD COLUMN added_seq BIGINT;
UPDATE period SET added_seq = NEXT VALUE FOR period_added_seq;
ALTER TABLE period ALTER COLUMN added_seq SET DEFAULT NEXT VALUE FOR period_added_seq;
