-- person_merged keeps a copy of person's columns (filled by SELECT p.*), so it takes the tags V71 gave person.
ALTER TABLE person_merged ADD COLUMN tags VARCHAR(2000) BEFORE merged_on;
