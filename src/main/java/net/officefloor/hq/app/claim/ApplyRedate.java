package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/**
 * A steward's re-date of an OPEN or PENDING claim: refused for a new date after its person's retirement date. Its
 * due-by stays as it was fixed and a figure a statement bound it to stays bound; as a claim belongs to the period of its
 * due-by, its period and number stay too.
 */
public class ApplyRedate {
    public void apply(@Val RedateTo to, @Val Claim claim, @Val ChangedBy by, ClaimRepository repository,
            PersonRepository people, RoleRepository roles) throws BadRequestException {
        String sheetId = claim.getSheetId();
        if (!Role.STEWARD.equals(roles.roleOf(sheetId, by.name()))) {
            throw new BadRequestException("Only a steward re-dates a claim");
        }
        if (!claim.getStatus().isOpen()) {
            throw new BadRequestException("Only an OPEN or PENDING claim is re-dated; this one is " + claim.getStatus());
        }
        LocalDate oldDate = claim.getDate();
        LocalDate newDate = to.date();
        RefuseRetiredDate.refuse(people.find(sheetId, claim.getPersonId()), newDate);
        if (!repository.redate(claim, newDate)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        to.redated(oldDate);
    }
}
