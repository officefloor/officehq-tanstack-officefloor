-- The people a claim is shared with, in the order shared (by id): each holds a share of its due, a percent with two decimals.
CREATE TABLE claim_share (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet  VARCHAR(200)  NOT NULL,
    claim  BIGINT        NOT NULL REFERENCES claim (id),
    person BIGINT        NOT NULL,
    share  DECIMAL(5, 2) NOT NULL
);
CREATE INDEX claim_share_sheet ON claim_share (sheet, claim, id);
