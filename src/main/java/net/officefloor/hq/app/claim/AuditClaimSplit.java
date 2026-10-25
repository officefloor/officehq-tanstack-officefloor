package net.officefloor.hq.app.claim;

import java.util.Objects;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the split, once it is saved: the original's line with the amount it keeps, then the new claim's number;
 *  then the new claim's opening. */
public class AuditClaimSplit {
    public void audit(@Parameter Split split, @Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        String by = AuditUser.decode(user);
        audit.record(ClaimAudit.line("CLAIM_SPLIT", claim, person, by) + " into=" + Objects.requireNonNullElse(split.made().getNumber(), ""));
        audit.record(ClaimAudit.line(ClaimStatus.OPEN.event(), split.made(), person, by));
    }
}
