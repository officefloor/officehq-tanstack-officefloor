-- Each cloned sheet's source: the sheet it was cloned from, by the clone's id. A grade with no rate in force on the
-- clone nor on its parents takes the source's (and so on up the source's parents and source). Not dated.
CREATE TABLE sheet_source (
    child  VARCHAR(200) PRIMARY KEY,
    source VARCHAR(200) NOT NULL
);
