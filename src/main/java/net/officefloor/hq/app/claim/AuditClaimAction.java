package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Records the claim's status change, with its person's name; runs only once the change has been allowed.
 * Settling and approving also record the due fixed (n/a when the grade had no rate); rejecting records its reason;
 * re-settling records the previous due and the new one; voiding a SETTLED claim records the frozen due it reverses; voiding with a reason records it last.
 */
public class AuditClaimAction {
    public void audit(@Val ClaimAction action, @Val ClaimNumber number, @Val Claim claim, @Val ClaimActionRequest request,
            @Val PreviousDue previous,
            PersonRepository people,
            @RequestParam("user") String user, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record(action.event() + " sheet=" + claim.sheetId() + " by=" + user + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().setScale(2).toPlainString()
                + " date=" + claim.date() + Claim.auditNumber(number.value())
                + (action == ClaimAction.RESETTLE
                        ? " old=" + figure(previous.value()) + " new=" + figure(claim.due())
                        : action.settles() ? " due=" + figure(claim.due()) : "")
                + (action == ClaimAction.REJECT ? " reason=" + request.reason() : "")
                + (action == ClaimAction.VOID && previous.settled() ? " reversed=" + figure(previous.value()) : "")
                + (action == ClaimAction.VOID && !request.reason().isEmpty() ? " reason=" + request.reason() : ""));
    }

    private static String figure(BigDecimal due) {
        return due == null ? "n/a" : due.setScale(2).toPlainString();
    }
}
