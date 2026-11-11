-- A plan's amount changes, oldest first: changed on the sheet's today to a new amount the claim form
-- would take. plan.amount is the amount now; claims generated from then on take it, claims already
-- generated keep theirs.
CREATE TABLE plan_change (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plan(id),
    changed_on DATE NOT NULL,
    amount DECIMAL(19, 2) NOT NULL
);
