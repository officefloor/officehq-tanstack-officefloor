-- Every state a sheet's claim, person, dated grade, period, patron, watch, role, delegation and statement has been
-- saved in, with the sheet's today when it was saved, in the order saved: the last one saved while the sheet's today
-- was on or before a date gives the thing as it stood at the end of that date (an as-of view). A null state: it was
-- deleted then. A thing with no state recorded was saved before this was kept, and stands as it is as of any date.
CREATE TABLE entity_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id VARCHAR(200) NOT NULL,
    kind VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    made_on DATE NOT NULL,
    state CLOB
);
CREATE INDEX entity_history_sheet ON entity_history (sheet_id, kind, id);
-- The sheet's today when it was archived (null: not archived, or not recorded).
ALTER TABLE sheet ADD COLUMN archived_on DATE;
