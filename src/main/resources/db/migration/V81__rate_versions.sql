-- A rate is kept as versions: setting a rate for a grade and from-date that already exist keeps the
-- value it replaces as an older version and adds a new one. `rate` holds the newest version (the one
-- in force) with its version number, 1 for the first; `rate_version` holds the older ones.
ALTER TABLE public.rate ADD COLUMN version INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE asof.rate ADD COLUMN version INTEGER DEFAULT 1 NOT NULL;
ALTER TABLE asof_hist.rate ADD COLUMN version INTEGER DEFAULT 1 NOT NULL;
-- The history trigger keeps the column list it was created with: create it again to see the new one.
DROP TRIGGER public.rate_history;
CREATE TRIGGER rate_history AFTER INSERT, UPDATE, DELETE ON public.rate FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE rate_version (
    sheet_id  VARCHAR(200) NOT NULL,
    grade     VARCHAR(1)   NOT NULL,
    from_date DATE,
    from_key  DATE GENERATED ALWAYS AS (COALESCE(from_date, DATE '0001-01-01')),
    version   INTEGER      NOT NULL,
    pct       DECIMAL(7,2) NOT NULL,
    to_date   DATE
);
CREATE UNIQUE INDEX rate_version_key ON rate_version (sheet_id, grade, from_key, version);
