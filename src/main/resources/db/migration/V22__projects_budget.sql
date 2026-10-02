-- A project can carry a budget: the figure the owner has agreed to spend on it, against which the
-- amount invoiced so far is measured. A new nullable NUMERIC column — a project without one set
-- simply has no budget yet, so every existing row stays untouched (NULL = no budget).
ALTER TABLE projects ADD COLUMN budget NUMERIC(12, 2);
