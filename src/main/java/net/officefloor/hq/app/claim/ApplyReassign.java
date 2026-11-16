package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Checks a move of a claim to another person on the sheet. Only a role the sheet's reassign permission allows (a steward while it is blank) moves a claim, and only a DRAFT, OPEN or
 * PENDING one: a SETTLED or VOID claim stays. A claim whose due-by falls inside a closed period, a correction (it belongs with the
 * claim it corrects), a move to the claim's own person, to a person not on the sheet, or to one retired before the
 * claim's date, is refused.
 */
public class ApplyReassign {
    public void apply(@Val Claim claim, @Val ReassignRequest request, PersonRepository people, RoleRepository roles,
            SettingRepository settings,             PeriodRepository periods, Out<ClaimReassignment> reassignment) throws InvalidClaimStatusException {
        if (!settings.permits(claim.sheetId(), "reassign", roles.roleOf(claim.sheetId(), request.by()))) {
            throw new InvalidClaimStatusException("The sheet's permission does not let this user move a claim");
        }
        if (claim.status() != ClaimStatus.DRAFT && claim.status() != ClaimStatus.OPEN
                && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot move");
        }
        if (claim.corrects() != null) {
            throw new InvalidClaimStatusException("A correction stays with the claim it corrects");
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
        Person from = people.find(claim.sheetId(), claim.personId());
        Person to = people.find(claim.sheetId(), request.personId());
        if (from == null || to == null || to.id() == from.id()) {
            throw new InvalidClaimStatusException("The claim must move to another person on the sheet");
        }
        if (!to.mayClaimOn(claim.date())) {
            throw new InvalidClaimStatusException("The claim's date is after " + to.name() + "'s retirement");
        }
        reassignment.set(new ClaimReassignment(from, to, request.by()));
    }
}
