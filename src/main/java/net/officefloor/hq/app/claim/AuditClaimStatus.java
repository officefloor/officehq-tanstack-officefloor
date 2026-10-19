package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim's status change once it is allowed and saved; a settle or approve adds the due it fixed
 *  (n/a without a rate), a reject its reason. A settle the clerk limit sent to PENDING is CLAIM_PENDING, with no due. */
public class AuditClaimStatus {
    public void audit(@Val ClaimAction action, @Val Claim claim, @Val ClaimNumber number,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "reason", required = false) String reason, PersonRepository people, Audit audit) {
        String event = action == ClaimAction.SETTLE && claim.getStatus() == ClaimStatus.PENDING
                ? ClaimAction.PENDING_EVENT : action.event();
        Person person = people.findById(claim.getSheetId(), claim.getPersonId());
        audit.record(event + " sheet=" + claim.getSheetId() + " by=" + user + " person=" + person.getName()
                + " amount=" + claim.getAmount().toPlainString() + " date=" + claim.getDate()
                + number.auditField()
                + (claim.getStatus() == ClaimStatus.SETTLED ? " due=" + (claim.getSettledDue() == null ? "n/a"
                        : claim.getSettledDue().toPlainString()) : "")
                + (action == ClaimAction.REJECT ? " reason=" + reason.trim() : ""));
    }
}
