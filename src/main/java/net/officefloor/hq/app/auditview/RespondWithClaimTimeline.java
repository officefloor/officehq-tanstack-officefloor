package net.officefloor.hq.app.auditview;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimResponse;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaimTimeline {
    // The claim's audit lines, newest first: by its number when it has one, else by its person, amount and date together.
    public void respond(@Val Claim claim, Audit audit, People people, ObjectResponse<List<String>> response) {
        Person person = people.find(claim.sheetId(), claim.personId());
        ClaimResponse shown = ClaimResponse.from(claim);
        response.send(audit.claimLines(claim.sheetId(), claim.number(), person == null ? "" : person.name(),
                String.valueOf(shown.amount()), String.valueOf(shown.date())));
    }
}
