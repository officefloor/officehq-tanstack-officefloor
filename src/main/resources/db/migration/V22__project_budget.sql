-- A project can carry a budget: the money planned for it, so the user can see how much has been
-- invoiced against it and what is left. Additive column with a default of 0, so existing rows and
-- any caller that omits it start with no budget set. NUMERIC(12, 2) mirrors the invoice amount
-- column so budget and invoiced totals share a scale.
ALTER TABLE project ADD COLUMN budget NUMERIC(12, 2) NOT NULL DEFAULT 0;
