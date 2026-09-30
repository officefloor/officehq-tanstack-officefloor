-- A project can carry a budget: the money planned for it, against which invoices are tracked. Its
-- detail page shows the budget, how much has been invoiced so far, and what is left. Nullable — a
-- project without a set budget has none; existing projects start with no budget until one is set.
ALTER TABLE projects ADD COLUMN budget NUMERIC(19, 2);
