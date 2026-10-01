-- The clients list can now be ordered by name. That read walks the clients by name, so index the
-- name column — an additive index, no data change, matching the name-order read path the sort leans
-- on (ListClientsSorted).
CREATE INDEX idx_clients_name ON clients (name);
