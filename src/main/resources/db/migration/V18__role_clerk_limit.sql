-- The sheet's roles: a user name (as signed in, trimmed) and its role, clerk or steward. A name with no row
-- is a clerk.
CREATE TABLE sheet_role (
    sheet_id  VARCHAR(100) NOT NULL REFERENCES sheet(id),
    user_name VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL CHECK (role IN ('clerk', 'steward')),
    PRIMARY KEY (sheet_id, user_name)
);
-- The sheet's clerk limit (two decimals; NULL: no limit). A clerk may settle a claim only when its due is at or
-- below it (a due of n/a is within any limit); a steward settles anything.
ALTER TABLE sheet ADD COLUMN clerk_limit DECIMAL(14, 2);
