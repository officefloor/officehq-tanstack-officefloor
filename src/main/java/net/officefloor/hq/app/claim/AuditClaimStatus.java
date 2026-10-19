package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim's status change once it is allowed and saved; a settle adds the due it fixed (n/a without a rate). */
public class AuditClaimStatus {
    public void audit(@Val ClaimAction action, @Val Claim claim, @Val ClaimNumber number,
            @RequestParam(value = "user", required = false) String user, PersonRepository people, Audit audit) {
        Person person = people.findById(claim.getSheetId(), claim.getPersonId());
        audit.record(action.event() + " sheet=" + claim.getSheetId() + " by=" + user + " person=" + person.getName()
                + " amount=" + claim.getAmount().toPlainString() + " date=" + claim.getDate()
                + number.auditField()
                + (action == ClaimAction.SETTLE ? " due=" + (claim.getSettledDue() == null ? "n/a"
                        : claim.getSettledDue().toPlainString()) : ""));
    }
}
