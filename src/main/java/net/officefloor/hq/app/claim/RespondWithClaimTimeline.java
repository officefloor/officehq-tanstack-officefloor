package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The claim's timeline: the audit lines about it exactly as the file holds them, newest first (see Audit#claimLines),
 *  read from the file: the viewer is not a second record. */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, PersonRepository people, Audit audit, ObjectResponse<List<String>> response) {
        Person person = people.find(claim.sheetId(), claim.personId());
        response.send(audit.claimLines(claim.sheetId(), claim.number(), person == null ? "" : person.name(),
                claim.amount(), claim.date()));
    }
}
