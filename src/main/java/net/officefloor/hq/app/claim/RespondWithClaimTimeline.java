package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.audit.AuditLinesResponse;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * A claim's timeline: the audit lines about it read from the audit file (it is not a second record),
 * newest first. A numbered claim's are those carrying its number; an unnumbered claim's are those
 * with no number naming its person, amount and date together.
 */
public class RespondWithClaimTimeline {
    public void respond(@Val Sheet sheet, @Val Claim claim, Audit audit, PersonRepository people,
            ObjectResponse<AuditLinesResponse> response) {
        Person person = people.find(sheet.id(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        List<String> lines = new ArrayList<>(audit.claimLinesFor(sheet.id(), claim.getNumber(),
                person == null ? "" : person.getName(), shown.amount(), shown.date()));
        Collections.reverse(lines);
        response.send(new AuditLinesResponse(lines));
    }
}
