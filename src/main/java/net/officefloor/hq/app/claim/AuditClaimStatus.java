package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the claim's status change, once it is saved, with the claim's number when it has one;
 * a settle adds the due it fixed (n/a when the grade had no rate). */
public class AuditClaimStatus {
    public void audit(@Parameter Integer number, @Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        String line = ClaimAudit.line(claim.getStatus().event(), claim, person, AuditUser.decode(user), number);
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getDue() == null ? "n/a" : claim.getDue().setScale(2).toPlainString());
        }
        audit.record(line);
    }
}
