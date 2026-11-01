package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a comment to a claim, kept with the user who wrote it and the sheet's today: COMMENT_ADDED is written naming
 * the claim and ending text=<text>. An empty comment is refused.
 */
public class CommentClaim {
    public void comment(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody CommentRequest request, @Val Claim claim, Claims claims, People people, SheetClock clock,
            Audit audit) throws InvalidClaimStatusChangeException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidClaimStatusChangeException("A comment cannot be empty");
        }
        claims.comment(sheet, claim.id(), user, clock.today(sheet), text);
        Person person = people.find(sheet, claim.person());
        audit.record("COMMENT_ADDED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " text=" + text);
    }
}
