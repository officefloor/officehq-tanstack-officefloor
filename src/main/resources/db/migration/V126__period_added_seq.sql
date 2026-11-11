-- The order a sheet's periods were added in: a larger one added later.
ALTER TABLE period ADD COLUMN added_seq BIGINT AUTO_INCREMENT;
