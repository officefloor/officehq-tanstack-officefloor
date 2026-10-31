package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonNotFoundException;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;

/** Moves a DRAFT, OPEN or PENDING claim to another person on the sheet; only a steward may. The
 *  claim keeps its number and date but loses any figure a statement bound it to: it is live again
 *  until its new person's next statement. A SETTLED or VOID claim cannot move, nor a claim dated in
 *  a closed period, nor one dated after its new person's retirement date. */
public class ApplyClaimReassign {
    public void apply(@Val ClaimReassignment change, @Val Claim claim, People people, Settings settings,
            ClosedPeriods closedPeriods)
            throws ClaimStatusRefusedException, PersonNotFoundException, PersonRetiredException, PeriodClosedException {
        if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can reassign a claim");
        }
        if (claim.getStatus() != ClaimStatus.DRAFT && !claim.getStatus().isOpen()) {
            throw new ClaimStatusRefusedException("A " + claim.getStatus() + " claim cannot be reassigned");
        }
        Person to = people.find(claim.getSheetId(), change.toPersonId());
        if (to == null) throw new PersonNotFoundException(change.toPersonId());
        if (to.getId() == claim.getPersonId()) {
            throw new ClaimStatusRefusedException("The claim is already " + to.getName() + "'s: reassign it to another person");
        }
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        to.refuseRetired(claim.getDate());
        Person from = people.find(claim.getSheetId(), claim.getPersonId());
        change.moved(from.getName(), to.getName());
        claim.setPersonId(to.getId());
        claim.setStated(false);
        claim.setStatedDue(null);
    }
}
