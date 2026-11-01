package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The claim's timeline (see {@link Audit#timelineFor}): its audit lines, newest first. */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, People people, Audit audit, ObjectResponse<List<String>> response) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        response.send(audit.timelineFor(claim.getSheetId(), claim.getNumber(),
                person == null ? null : person.getName(), claim.getAmount().toPlainString(),
                String.valueOf(claim.getDate())));
    }
}
