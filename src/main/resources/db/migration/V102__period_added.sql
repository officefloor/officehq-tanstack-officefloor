-- The order the sheet's periods were added in: each new period takes the next number.
ALTER TABLE period ADD COLUMN added_seq BIGINT AUTO_INCREMENT;
