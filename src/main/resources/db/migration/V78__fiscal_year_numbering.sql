-- Sheet-wide numbering restarts each fiscal year: a claim opened (when not numbered per period) is numbered
-- FY<yyyy>-<n>, counting from 1 within the fiscal year (by the opening sheet's fiscal start month) its due-by falls
-- in, the sequence shared across the sheet's tree. Each sheet's last number given in a fiscal year is kept in
-- claim_sequence under seq_key 'FY<yyyy>'; the tree's next is one after the largest of those. Numbers already
-- given stay; sheet.claim_number is no longer advanced.
SELECT 1;
