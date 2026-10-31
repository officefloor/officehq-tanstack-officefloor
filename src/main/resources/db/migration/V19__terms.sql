-- The sheet's terms, in days: a claim is due by its date plus them. Null (or no row) is the default, 14.
ALTER TABLE sheet_setting ADD COLUMN terms INT;
