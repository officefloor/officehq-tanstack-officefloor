package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/**
 * The claim's timeline: the sheet's audit lines about it, newest first, as the file holds them — those carrying its
 * number, or for a claim with no number those naming its person, amount and date together.
 */
public class RespondWithClaimTimeline {
    public void respond(@Val Claim claim, @Val Person person, Audit audit, ObjectResponse<List<String>> response) {
        response.send(audit.timeline(claim.sheetId(), claim.number(), person.name(),
                claim.amount().setScale(2).toPlainString(), claim.date().toString()));
    }
}
