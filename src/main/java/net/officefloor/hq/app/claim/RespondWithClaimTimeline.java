package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The claim's timeline: the audit lines about it, newest first, as the file holds them (see {@link Audit#timeline}). */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, PersonStore people, Audit audit, ObjectResponse<List<String>> response) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        response.send(audit.timeline(claim.getSheetId(), claim.getNumber(), person == null ? "" : person.getName(),
                shown.amount(), shown.date()));
    }
}
