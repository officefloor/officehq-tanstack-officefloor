package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.RequestBody;

public class ApplyReassign {
    // Only a role the permission matrix allows to reassign (by default a steward) moves a claim, and only a DRAFT, OPEN or PENDING one, to a person on the sheet who was not
    // retired by its date (its own person too: it then lists first under them again, as when a bulk move takes it there). A correction stays with the person whose settled claim it corrects. Stored only if the
    // claim still has the person and status it was loaded with.
    public void apply(@RequestBody(required = false) ReassignRequest body, @Val Claim claim, Claims claims,
            People people, Settings settings, ServerHttpConnection connection, Out<ClaimResponse> responseOut,
            Out<Person> toOut) throws ClaimReassignRefusedException, ClaimAfterRetirementException {
        String user = Audit.user(connection).trim();
        if (!settings.permits(claim.sheetId(), "reassign", user)) {
            throw new ClaimReassignRefusedException("Your role is not allowed to move a claim to another person.");
        }
        if (!(claim.status() == ClaimStatus.DRAFT || claim.status().isOpen())) {
            throw new ClaimReassignRefusedException("A " + claim.status() + " claim cannot move to another person.");
        }
        if (claim.isCorrection()) {
            throw new ClaimReassignRefusedException("A correction stays with the person whose claim it corrects.");
        }
        Person to = body == null || body.personId() == null ? null : people.find(claim.sheetId(), body.personId());
        if (to == null) {
            throw new ClaimReassignRefusedException("Choose a person on this sheet to move the claim to.");
        }
        RefuseRetiredInput.check(to, claim.date());
        Claim moved = claims.reassign(claim, to.id());
        if (moved == null) {
            throw new ClaimReassignRefusedException("The claim has changed since it was loaded.");
        }
        responseOut.set(ClaimResponse.from(moved));
        toOut.set(to);
    }
}
