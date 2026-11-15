package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a status change that went through is recorded. */
public class RecordClaimStatus {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Claim claim, PersonRepository people, Audit audit) {
        audit.record(line(ClaimStatus.valueOf(claim.getStatus()).event(), by, claim, people)
                + (ClaimStatus.SETTLED.name().equals(claim.getStatus()) ? due(claim) : "")
                + (claim.isReversal() ? reversed(claim) : "")
                + (ClaimStatus.VOID.name().equals(claim.getStatus()) && claim.getVoidReason() != null
                        ? " reason=" + claim.getVoidReason() : "")
                + (ClaimStatus.SETTLED.name().equals(claim.getStatus()) && claim.getSettledAsOf() != null
                        ? " on=" + claim.getSettledAsOf() : ""));
    }

    /** A claim line: the event, the sheet, the user, then the claim's person, amount, date and number. */
    static String line(String event, String by, Claim claim, PersonRepository people) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        return event + " sheet=" + claim.getSheetId()
                + " by=" + (by == null ? "" : by)
                + " person=" + (person == null ? "" : person.getName())
                + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                + claim.getAuditNumber();
    }

    /** The reversed suffix of a voided SETTLED claim: the frozen figure that left the totals. */
    static String reversed(Claim claim) {
        return " reversed=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue());
    }

    /** The fixed due suffix of a settled claim. */
    static String due(Claim claim) {
        return " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue());
    }
}
