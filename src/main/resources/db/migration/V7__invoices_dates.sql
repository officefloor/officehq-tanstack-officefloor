-- Invoices now carry two dates: issued_date (when it went out) and due_date (when payment is due),
-- so a project's detail page can show both on each invoice row. Existing rows adopt today's date as
-- issued and 30 days later as due via the column defaults; new rows set them explicitly on create.
ALTER TABLE invoices ADD COLUMN issued_date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE invoices ADD COLUMN due_date DATE NOT NULL DEFAULT DATEADD('DAY', 30, CURRENT_DATE);
