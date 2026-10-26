package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * A steward's re-settle of a SETTLED claim: freezes its due again at the figure it would have today as an OPEN claim bound
 * by no statement — the rate in force on the claim's date of its person's grade on that date, with its late charge as of the sheet's
 * today on top, and at least the sheet's floor in force on the claim's date. Without a rate its due becomes n/a.
 */
public class ApplyResettle {
    public void apply(@Val Claim claim, @Val ChangedBy by, ClaimRepository repository, PersonRepository people,
            RateRepository rates, RoleRepository roles, SettingRepository settings, SheetRepository sheets,
            Out<Resettled> resettled) throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(claim.getSheetId(), by.name()))) {
            throw new BadRequestException("Only a steward re-settles a claim");
        }
        if (claim.getStatus() != ClaimStatus.SETTLED) {
            throw new BadRequestException("Only a SETTLED claim is re-settled; this one is " + claim.getStatus());
        }
        LateCharge late = settings.lateCharge(claim.getSheetId(), sheets.today(claim.getSheetId()));
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal rate = person == null ? null
                : rates.find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate(), claim.getAddedSeq());
        BigDecimal figure = claim.figureAt(rate, late.rounding(claim));
        BigDecimal before = claim.getSettledDue();
        BigDecimal after = late.dueAsOpen(claim, figure);
        claim.setSettledDue(after);
        if (!repository.updateStatus(claim, ClaimStatus.SETTLED)) {
            throw new BadRequestException("The claim was changed by someone else; reload the sheet");
        }
        resettled.set(new Resettled(before, after));
    }
}
