package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/**
 * Stores the batch a settle puts the claim in: a settle (or re-settle) that is part of a bulk settle (the body's batch
 * token) gives it that bulk settle's batch number — the sheet's next the first time — whether it ends SETTLED or
 * PENDING; a settle taken one at a time gives it none, while a re-settle taken one at a time keeps the batch it has.
 * A reject (back to OPEN) clears it; other changes keep it.
 */
public class SaveClaimBatch {
    public void save(@Val ClaimAction action, @Val Claim claim, @Val ClaimActionRequest request,
            SettleBatchRepository batches) {
        switch (action) {
            case SETTLE, PEND -> batches.saveBatch(claim.sheetId(), claim.id(),
                    request.batch().isEmpty() ? null : batches.numberFor(claim.sheetId(), request.batch()));
            case RESETTLE -> {
                if (!request.batch().isEmpty()) {
                    batches.saveBatch(claim.sheetId(), claim.id(), batches.numberFor(claim.sheetId(), request.batch()));
                }
            }
            case REJECT -> batches.saveBatch(claim.sheetId(), claim.id(), null);
            default -> {
            }
        }
    }
}
