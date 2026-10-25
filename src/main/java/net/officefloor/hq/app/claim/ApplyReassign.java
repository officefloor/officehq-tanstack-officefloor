package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to another person on the sheet: a steward's, and only for a DRAFT, OPEN or PENDING claim, never to its
 * own person nor to one retired before the claim's date. It keeps its number and date, loses any figure a statement
 * bound it to (live again until its new person's next statement) and is listed as its new person's newest claim.
 */
public class ApplyReassign {
    public void apply(@Val ReassignTo to, @Val Claim claim, @Val ChangedBy by, ClaimRepository repository,
            PersonRepository people, RoleRepository roles) throws BadRequestException {
        String sheetId = claim.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by.name()))) {
            throw new BadRequestException("Only a steward moves a claim to another person");
        }
        ClaimStatus status = claim.getStatus();
        if (status != ClaimStatus.DRAFT && !status.isOpen()) {
            throw new BadRequestException("A " + status + " claim cannot move to another person");
        }
        Person target = people.find(sheetId, to.personId());
        if (target == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (target.getId() == claim.getPersonId()) {
            throw new BadRequestException("The claim is already that person's");
        }
        RefuseRetiredDate.refuse(target, claim.getDate());
        Person from = people.find(sheetId, claim.getPersonId());
        if (!repository.reassign(claim, target.getId())) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        to.moved(from == null ? "" : from.getName(), target.getName());
    }
}
