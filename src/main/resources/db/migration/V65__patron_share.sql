-- The order a person named their patrons in (seq order): 100.00 is split equally among a person's patrons, each share
-- rounded half to even to two decimals, the remainder going to the first named. Existing patrons keep their old
-- (patron id) order.
ALTER TABLE person_patron ADD COLUMN seq BIGINT AUTO_INCREMENT;
