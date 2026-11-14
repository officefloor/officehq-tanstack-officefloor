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
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimStatus.valueOf(claim.getStatus()).event() + " sheet=" + claim.getSheetId()
                + " by=" + (by == null ? "" : by)
                + " person=" + (person == null ? "" : person.getName())
                + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                + claim.getAuditNumber()
                + (ClaimStatus.SETTLED.name().equals(claim.getStatus())
                        ? " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue()) : ""));
    }
}
