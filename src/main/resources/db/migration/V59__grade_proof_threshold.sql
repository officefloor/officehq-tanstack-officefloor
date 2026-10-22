-- A grade may have a proof threshold of its own: a dated setting (setting_value keys proof-threshold-A/B/C; blank:
-- none, the sheet's proof-threshold applies). A claim's threshold is its person's grade's (grade on the claim's date)
-- in force on its date when one is set, else the sheet's in force on its date. No schema change is needed.
SELECT 1;
