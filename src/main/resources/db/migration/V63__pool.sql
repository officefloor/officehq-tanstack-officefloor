-- The sheet's pools, in the order added: a name (trimmed, at most once per sheet).
CREATE TABLE pool (
    sheet_id VARCHAR(200) NOT NULL,
    name     VARCHAR(200) NOT NULL,
    position BIGINT       NOT NULL,
    PRIMARY KEY (sheet_id, name)
);

-- A pool's members, in the order added: a person of the sheet with a share percent (more than 0.00,
-- at most 100.00). A person is in a pool at most once, and may be in several pools.
CREATE TABLE pool_member (
    sheet_id  VARCHAR(200)  NOT NULL,
    pool      VARCHAR(200)  NOT NULL,
    person_id BIGINT        NOT NULL REFERENCES person (id),
    share     DECIMAL(5,2)  NOT NULL,
    position  BIGINT        NOT NULL,
    PRIMARY KEY (sheet_id, pool, person_id)
);

CREATE TABLE asof.pool AS SELECT * FROM public.pool WHERE FALSE;
CREATE TABLE asof_hist.pool AS SELECT * FROM public.pool WHERE FALSE;
ALTER TABLE asof_hist.pool ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.pool ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.pool ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX pool_hist_sheet ON asof_hist.pool (sheet_id, h_seq);
CREATE TRIGGER pool_history AFTER INSERT, UPDATE, DELETE ON public.pool FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';

CREATE TABLE asof.pool_member AS SELECT * FROM public.pool_member WHERE FALSE;
CREATE TABLE asof_hist.pool_member AS SELECT * FROM public.pool_member WHERE FALSE;
ALTER TABLE asof_hist.pool_member ADD COLUMN h_seq BIGINT NOT NULL;
ALTER TABLE asof_hist.pool_member ADD COLUMN h_made_on DATE;
ALTER TABLE asof_hist.pool_member ADD COLUMN h_op CHAR(1) NOT NULL;
CREATE INDEX pool_member_hist_sheet ON asof_hist.pool_member (sheet_id, h_seq);
CREATE TRIGGER pool_member_history AFTER INSERT, UPDATE, DELETE ON public.pool_member FOR EACH ROW CALL 'net.officefloor.hq.app.sheet.History';
