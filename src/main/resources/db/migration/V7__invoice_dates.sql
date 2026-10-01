-- An invoice now carries two dates: when it was issued (went out) and when it is due. Both are plain
-- calendar dates (DATE, no time-of-day), so the UI shows a bare YYYY-MM-DD. New and existing rows get
-- CURRENT_DATE as a safe default; the test seed supplies explicit dates, and CreateInvoice sets its
-- own, so the default only guards rows raised before this column existed.
ALTER TABLE invoices ADD COLUMN issued_date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE invoices ADD COLUMN due_date DATE NOT NULL DEFAULT CURRENT_DATE;
