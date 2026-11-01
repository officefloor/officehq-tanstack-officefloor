-- A sheet a steward has archived: everything on it stays visible and nothing on it changes again.
CREATE TABLE sheet_archive (
    sheet        VARCHAR(200) PRIMARY KEY,
    archived_by  VARCHAR(200) NOT NULL
);
