-- The single reference date the dashboard measures "overdue" against. In a real deploy this is
-- today; the harness seeds a fixed date (asOf) so the overdue count is deterministic. Exactly one
-- row (id = 1) holds the date; absent means "use today". Not an IDENTITY column — the row's id is a
-- fixed constant, not generated.
CREATE TABLE dashboard_reference (
    id    BIGINT NOT NULL PRIMARY KEY,
    as_of DATE NOT NULL
);
