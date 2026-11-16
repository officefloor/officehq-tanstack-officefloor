-- Each policy version keeps the value its change set (null: blank).
ALTER TABLE policy_version ADD COLUMN val VARCHAR(200);
-- The versions already made take theirs from the n-th change to a policy setting, in the order made.
UPDATE policy_version pv SET val = (
    SELECT h.val FROM (
        SELECT sheet_id, val, ROW_NUMBER() OVER (PARTITION BY sheet_id ORDER BY id) AS n
        FROM setting_history
        WHERE setting IN ('rounding', 'floor', 'cap', 'terms', 'late-pct', 'clerk-limit')
    ) h WHERE h.sheet_id = pv.sheet_id AND h.n = pv.version);
