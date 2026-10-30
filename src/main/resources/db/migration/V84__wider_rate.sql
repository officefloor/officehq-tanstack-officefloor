-- A rate may have up to seven whole digits (a percent of 12345.67, say), not four.
ALTER TABLE rate ALTER COLUMN rate DECIMAL(9,2) NOT NULL;
ALTER TABLE rate_version ALTER COLUMN rate DECIMAL(9,2) NOT NULL;
ALTER TABLE rate_history ALTER COLUMN rate DECIMAL(9,2) NOT NULL;
