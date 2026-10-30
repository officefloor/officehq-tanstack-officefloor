package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The claim's timeline: its audit lines as the file holds them, newest first — those carrying its number, or for a
 * claim with no number those naming its person, amount and date together.
 */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, People people, Audit audit, ObjectResponse<List<String>> response) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        response.send(audit.claimLines(claim.getSheetId(), claim.getNumber(),
                person == null ? "" : person.getName(), String.valueOf(shown.amount()), String.valueOf(shown.date())));
    }
}
