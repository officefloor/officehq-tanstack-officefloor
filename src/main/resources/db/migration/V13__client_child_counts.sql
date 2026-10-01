-- The client detail page shows how many projects and contacts a client has, counted per client_id
-- (GetClientSummary -> countByClientId). Index the two FK columns those count queries filter on so
-- the aggregate is served from an index rather than a full scan. Additive and local: new indexes,
-- no table or column change.
CREATE INDEX idx_projects_client_id ON projects (client_id);
CREATE INDEX idx_contacts_client_id ON contacts (client_id);
