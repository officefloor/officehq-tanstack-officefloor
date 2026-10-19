package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the move once it is allowed and saved, naming the claim by its new person like every claim line. */
public class AuditClaimReassigned {
    public void audit(@Val Claim claim, @Val Reassignment reassignment,
            @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("CLAIM_REASSIGNED sheet=" + claim.getSheetId() + " by=" + user + " person=" + reassignment.to()
                + " amount=" + claim.getAmount().toPlainString() + " date=" + claim.getDate()
                + new ClaimNumber(claim.getNumber()).auditField()
                + " from=" + reassignment.from() + " to=" + reassignment.to());
    }
}
