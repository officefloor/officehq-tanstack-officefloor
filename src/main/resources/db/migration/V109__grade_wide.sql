-- A grade added to a sheet is a code of up to three capital letters (see V108), so every grade column holds one.
ALTER TABLE person ALTER COLUMN grade SET DATA TYPE VARCHAR(20);
ALTER TABLE person_grade ALTER COLUMN grade SET DATA TYPE VARCHAR(20);
ALTER TABLE rate ALTER COLUMN grade SET DATA TYPE VARCHAR(20);
ALTER TABLE rate_log ALTER COLUMN grade SET DATA TYPE VARCHAR(20);
ALTER TABLE rate_version ALTER COLUMN grade SET DATA TYPE VARCHAR(20);
