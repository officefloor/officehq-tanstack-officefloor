-- A closed or soft-closed period's cut per pool member, fixed as it stood when the period was (soft-)closed: one row
-- for every member of every pool then, the member being the person's id or pool:<name>, the cut '' when they had none.
-- A member added later has none (and shows blank); reopening the period drops its rows.
CREATE TABLE period_pool_cut (
    sheet  VARCHAR(200) NOT NULL,
    period VARCHAR(200) NOT NULL,
    pool   VARCHAR(200) NOT NULL,
    member VARCHAR(200) NOT NULL,
    cut    VARCHAR(40)  NOT NULL,
    PRIMARY KEY (sheet, period, pool, member)
);
