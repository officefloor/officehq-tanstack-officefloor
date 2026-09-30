-- Global search looks across projects by name (GET /api/search): an index on project.name keeps
-- that name lookup fast as the list grows, mirroring idx_client_name (V6) on the client side.
-- Additive index, no data change; existing rows and the create path are unaffected.
CREATE INDEX idx_project_name ON project (name);
