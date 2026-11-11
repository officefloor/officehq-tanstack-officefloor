-- Whether the period was locked as of each change kept in its history (a lock or unlock is kept too).
-- NULL on rows kept before: read as the period's locked now.
ALTER TABLE period_history ADD COLUMN locked BOOLEAN;
