-- A project can be marked ACTIVE, ON_HOLD or FINISHED so its state is visible on the list. Additive
-- column with a default, so existing rows and any caller that omits it start ACTIVE. The CHECK pins
-- the column to the three known values, mirroring the other value-guard migrations.
ALTER TABLE project ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE project ADD CONSTRAINT project_status_valid
    CHECK (status IN ('ACTIVE', 'ON_HOLD', 'FINISHED'));
