-- A project can carry a BUDGET: the planned spend the user sets against it, which the detail page
-- compares with how much has been invoiced. It is money, so a fixed-scale DECIMAL (two places)
-- rather than a float. Nullable because a budget is optional — a project without one has not had a
-- budget set yet, and existing projects keep NULL until the user sets one.
ALTER TABLE projects ADD COLUMN budget DECIMAL(12, 2);
