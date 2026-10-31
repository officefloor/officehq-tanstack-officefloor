-- A period can be soft-closed: its due-total and each person's are fixed as by a close
-- (`closed` is TRUE, `closed_total` and the period_person_total rows set), but `soft` is TRUE, so
-- claims dated in it can still be added, edited and change status.
ALTER TABLE period ADD COLUMN soft BOOLEAN DEFAULT FALSE NOT NULL;
-- Whether the claim was added while its period was soft-closed.
ALTER TABLE claim ADD COLUMN late_entry BOOLEAN DEFAULT FALSE NOT NULL;
