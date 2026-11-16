package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-dates an OPEN or PENDING claim, by a steward. Its due-by stays as it was fixed and a figure a statement bound it
 * to stays bound; its period follows the new date. While per-period numbering is on (see ClaimNumbering) and the new
 * date falls in another period's sequence than the old one, the claim takes the next number of its new sequence (its
 * old number is never given again). Anyone but a steward, any other claim, a claim dated inside a closed period, a new
 * date inside one, or a new date after the person's retirement, is refused.
 */
public class ApplyRedate {
    public void apply(@Val Claim claim, @Val ClaimRedating request, RoleRepository roles, PersonRepository people,
            PeriodRepository periods, SettingRepository settings, SheetRepository sheets, ClaimRepository claims,
            Out<Claim> changed, Out<ClaimRedating> redating) throws InvalidClaimStatusException {
        String sheet = claim.sheetId();
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheet, request.by()))) {
            throw new InvalidClaimStatusException("Only a steward re-dates a claim");
        }
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be re-dated");
        }
        if (periods.closedOn(sheet, claim.date())) {
            throw new InvalidClaimStatusException("The claim's date is inside a closed period");
        }
        if (periods.closedOn(sheet, request.date())) {
            throw new InvalidClaimStatusException("The new date is inside a closed period");
        }
        Person person = people.find(sheet, claim.personId());
        if (person == null || !person.mayClaimOn(request.date())) {
            throw new InvalidClaimStatusException("The new date is after the person's retirement");
        }
        Claim redated = new Claim(claim.id(), sheet, claim.personId(), claim.amount(), request.date(), claim.note(),
                claim.status(), claim.number(), claim.due(), claim.openedBy(), claim.stated(), claim.statedDue(),
                claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom());
        String number = claim.number();
        if (ClaimNumbering.perPeriod(sheet, settings, sheets)
                && !keyOf(periods.containing(sheet, claim.date())).equals(keyOf(periods.containing(sheet, request.date())))) {
            number = ClaimNumbering.next(redated, claims, periods, settings, sheets);
        }
        changed.set(new Claim(claim.id(), sheet, claim.personId(), claim.amount(), request.date(), claim.note(),
                claim.status(), number, claim.due(), claim.openedBy(), claim.stated(), claim.statedDue(),
                claim.corrects(), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom()));
        redating.set(new ClaimRedating(request.date(), request.by(), claim.date()));
    }

    private static String keyOf(Period period) {
        return period == null ? "X" : period.name();
    }
}
