package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** CLAIM_RESETTLED, naming the claim, then its settled due before and after; and its SETTLED status log line. */
public class RecordClaimResettled {
    public void record(@Val Claim claim, @Val Resettled resettled, @Val ChangedBy by, PersonRepository people,
            Audit audit, ClaimLog log) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_RESETTLED", claim, by, person == null ? "" : person.getName())
                + " old=" + shown(resettled.before()) + " new=" + shown(resettled.after()));
        log.record(claim, by);
    }

    private static String shown(BigDecimal due) {
        return due == null ? "n/a" : due.toPlainString();
    }
}
