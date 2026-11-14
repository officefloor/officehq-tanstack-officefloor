package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, ClaimLogRepository logs, ClaimLockRepository locks,
            ClaimRepository repository, ClaimCommentRepository comments, ClaimLineRepository lines,
            ClaimShareRepository shares,
            SettleBatchRepository batches, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(claim, logs.find(claim.sheetId(), claim.id()),
                locks.lockedBy(claim.sheetId(), claim.id()),
                repository.lateEntry(claim.sheetId(), claim.id()),
                repository.reminded(claim.sheetId(), claim.id()),
                repository.ref(claim.sheetId(), claim.id()),
                repository.tags(claim.sheetId(), claim.id()),
                repository.voidReason(claim.sheetId(), claim.id()),
                comments.find(claim.sheetId(), claim.id()),
                batches.batch(claim.sheetId(), claim.id()),
                lines.find(claim.sheetId(), claim.id()),
                repository.photoProofs(claim.sheetId(), claim.id()),
                shares.find(claim.sheetId(), claim.id())));
    }
}
