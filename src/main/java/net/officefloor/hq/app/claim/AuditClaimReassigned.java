package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim just moved, with its new person's name, then the names it moved from and to. */
public class AuditClaimReassigned {
    public void audit(@Val Claim claim, @Val Reassignment reassignment, @RequestParam("user") String user,
            Audit audit) {
        String from = reassignment.from() == null ? "" : reassignment.from().name();
        audit.record("CLAIM_REASSIGNED sheet=" + claim.sheetId() + " by=" + user + " person="
                + reassignment.to().name() + " amount=" + claim.amount().setScale(2).toPlainString()
                + " date=" + claim.date() + Claim.auditNumber(claim.number()) + " from=" + from + " to="
                + reassignment.to().name());
    }
}
