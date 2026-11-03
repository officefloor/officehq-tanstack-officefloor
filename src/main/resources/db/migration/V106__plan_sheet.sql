-- A plan may name one of its sheet's descendants (a sheet whose parent, or that one's parent and so on, is the
-- plan's sheet); null when it names none. Its claims are then generated for the person of the same name on that
-- sheet (none when there is no such person), by that sheet's rules and written in that sheet's audit.
ALTER TABLE plan ADD COLUMN target_sheet_id VARCHAR(200);
