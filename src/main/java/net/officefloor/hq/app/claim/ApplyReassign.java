package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to another person on the sheet: by a role the sheet's reassign permission gives (a steward's while blank), and only for a DRAFT, OPEN or PENDING claim, never to its
 * own person nor to one retired before the claim's date. It keeps its date and, when numbered, takes the next number of
 * the sequence it would be numbered in (its old number is never used again; a draft gets none), loses any figure a statement
 * bound it to (live again until its new person's next statement) and is listed as its new person's newest claim.
 */
public class ApplyReassign {
    public void apply(@Val ReassignTo to, @Val Claim claim, @Val ChangedBy by, ClaimRepository repository,
            PersonRepository people, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            PeriodRepository periods) throws BadRequestException {
        move(to, claim, by, repository, people, roles, settings, sheets, periods, false);
    }

    /** Moves the claim; in a bulk move ({@code bulk}) one already the person's is moved (relisted) all the same. */
    void move(ReassignTo to, Claim claim, ChangedBy by, ClaimRepository repository, PersonRepository people,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets, PeriodRepository periods, boolean bulk) throws BadRequestException {
        String sheetId = claim.getSheetId();
        String role = roles.roleOf(sheetId, by.name());
        if (!settings.allows(sheetId, "reassign", role)) {
            throw new BadRequestException("A " + role + " may not move a claim to another person on this sheet");
        }
        ClaimStatus status = claim.getStatus();
        if (status != ClaimStatus.DRAFT && !status.isOpen()) {
            throw new BadRequestException("A " + status + " claim cannot move to another person");
        }
        Person target = people.find(sheetId, to.personId());
        if (target == null) {
            throw new BadRequestException("No such person on this sheet");
        }
        if (!bulk && target.getId() == claim.getPersonId()) {
            throw new BadRequestException("The claim is already that person's");
        }
        RefuseRetiredDate.refuse(target, claim.getDate());
        Person from = people.find(sheetId, claim.getPersonId());
        if (!repository.reassign(claim, target.getId())) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        if (claim.getNumber() != null && !claim.getNumber().isEmpty()) {
            // Numbered only once the claim is surely moving, so a refused move uses up no number.
            claim.setNumber(ApplyClaimStatus.number(claim, repository, settings, sheets, periods));
            repository.updateStatus(claim, claim.getStatus());
        }
        to.moved(from == null ? "" : from.getName(), target.getName());
    }
}
