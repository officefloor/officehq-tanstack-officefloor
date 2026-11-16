-- A cancelled statement is kept, with the sheet's today when it was cancelled (null: in force), so the
-- sheet read as of an earlier date still counts it. Only statements in force count now.
ALTER TABLE statement ADD COLUMN cancelled_on DATE;
