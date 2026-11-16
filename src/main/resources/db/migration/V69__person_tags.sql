-- A person's tags (null when they have none): each trimmed and in lower case, no repeats, sorted, joined by commas.
ALTER TABLE person ADD COLUMN tags VARCHAR(2000);
