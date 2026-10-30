-- Claims are listed newest first by listed_seq, not by id: a claim moved to another person is
-- listed as that person's newest claim, whatever its date. A new claim, and a moved one, takes the
-- next listed_seq after every claim's; claims already stored keep their order (their id).
ALTER TABLE claim ADD COLUMN listed_seq BIGINT;
UPDATE claim SET listed_seq = id;
