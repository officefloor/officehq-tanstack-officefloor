-- Searching clients by name: an index on client.name keeps the name lookup fast as the list grows.
-- Additive index, no data change; existing rows and the create path are unaffected.
CREATE INDEX idx_client_name ON client (name);
