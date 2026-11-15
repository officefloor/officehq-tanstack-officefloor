package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** A claim's timeline: the audit lines about it, newest first, as the file holds them — those carrying
 *  its number, or for a claim with no number those naming its person, amount and date together. */
public class ListClaimTimeline {
    public void list(@Val Claim claim, PersonRepository people, Audit audit,
            ObjectResponse<List<String>> response) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        response.send(audit.claimLines(claim.getSheetId(), claim.getNumber(),
                person == null ? "" : person.getName(), claim.getAmount(), claim.getDate()));
    }
}
