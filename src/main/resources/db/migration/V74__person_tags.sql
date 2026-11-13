-- A person's tags (null: none): lower-cased, sorted and comma separated, as a claim's. Each joins the sheet's tag list
-- (claim_tag), which keeps every tag ever given on the sheet.
ALTER TABLE person ADD COLUMN tags VARCHAR(1000);
