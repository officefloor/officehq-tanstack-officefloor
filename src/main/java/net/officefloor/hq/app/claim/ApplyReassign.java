package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves a DRAFT, OPEN or PENDING claim to another person on the sheet; only the roles the sheet's reassign permission allows
 * (by default a steward) move a claim, and none whose due-by is inside a closed period moves.
 * It keeps its number and date but loses any figure a statement bound it to: it is live again until
 * its new person's next statement.
 */
public class ApplyReassign {
    public void apply(@Val ReassignRequest request, @Val Claim claim, People people, Settings settings, Periods periods,
            Out<ClaimMove> move) throws ClaimStatusRefusedException {
        if (!settings.allows(claim.getSheetId(), request.user(), "reassign")) {
            throw new ClaimStatusRefusedException("Your role may not move a claim to another person");
        }
        ClaimStatus status = claim.getStatus();
        if (status != ClaimStatus.DRAFT && status != ClaimStatus.OPEN && status != ClaimStatus.PENDING) {
            throw new ClaimStatusRefusedException("A " + status + " claim cannot move to another person");
        }
        if (periods.isClosed(claim.getSheetId(), claim.getPeriodDate())) {
            throw new ClaimStatusRefusedException("No claim whose due-by is inside a closed period can move to another person");
        }
        Person to = null;
        try {
            to = people.find(claim.getSheetId(), Long.parseLong(request.toPersonId()));
        } catch (NumberFormatException e) {
            // not an id this app assigned
        }
        if (to == null) {
            throw new ClaimStatusRefusedException("No such person to move the claim to");
        }
        if (to.getId() == claim.getPersonId()) {
            throw new ClaimStatusRefusedException("The claim is already that person's");
        }
        Person from = people.find(claim.getSheetId(), claim.getPersonId());
        claim.setPersonId(to.getId());
        claim.setStated(false);
        claim.setStatedDue(null);
        move.set(new ClaimMove(from, to));
    }
}
