-- Sorting a project's invoices by due date is a first-class view now, so index the column the sort
-- orders by (scoped by project, matching the per-project list query). Additive: no data changes.
CREATE INDEX idx_invoices_project_due ON invoices (project_id, due_date);
