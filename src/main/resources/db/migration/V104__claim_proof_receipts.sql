-- How many of a claim's proofs are receipts: a proof added with no kind chosen while the sheet has no proof kinds
-- records no kind, so it is neither a receipt nor a photo (every proof added before this is a receipt or a photo).
ALTER TABLE claim ADD COLUMN proofs_receipt INT DEFAULT 0 NOT NULL;
UPDATE claim SET proofs_receipt = proofs - proofs_photo;
