package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Records the claim's status change, with its person's name; runs only once the change has been allowed.
 * Settling also records the due it fixed (n/a when the grade had no rate).
 */
public class AuditClaimAction {
    public void audit(@Val ClaimAction action, @Val ClaimNumber number, @Val Claim claim, PersonRepository people,
            @RequestParam("user") String user, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record(action.event() + " sheet=" + claim.sheetId() + " by=" + user + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().setScale(2).toPlainString()
                + " date=" + claim.date() + Claim.auditNumber(number.value())
                + (action == ClaimAction.SETTLE
                        ? " due=" + (claim.due() == null ? "n/a" : claim.due().setScale(2).toPlainString())
                        : ""));
    }
}
