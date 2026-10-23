-- A grade may have a clerk limit of its own: a dated setting (setting_value keys clerk-limit-A/B/C; blank: none, the
-- sheet's clerk-limit applies). For a claim, the limit in force is its person's grade's (grade on the sheet's today)
-- in force on the sheet's today when one is set, else the sheet's. No schema change is needed.
SELECT 1;
