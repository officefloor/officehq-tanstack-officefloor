package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the re-settle, once it is saved: the claim's line, then the due it had and the one it has now. */
public class AuditClaimResettled {
    public void audit(@Parameter Resettled resettled, @Val Claim claim, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String person = people.findById(claim.getPersonId()).map(Person::getName).orElse("");
        audit.record(ClaimAudit.line("CLAIM_RESETTLED", claim, person, AuditUser.decode(user))
                + " old=" + shown(resettled.old()) + " new=" + shown(resettled.now()));
    }

    private static String shown(BigDecimal due) {
        return due == null ? "n/a" : due.setScale(2).toPlainString();
    }
}
