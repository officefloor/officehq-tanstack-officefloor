package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.Dues;
import net.officefloor.hq.app.statement.LateCharge;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to its new status, refusing a change its status does not allow (or one
 *  another user's change got to first). Opening gives the claim the sheet's next number; voiding
 *  keeps it (a number is never reused). Opening records who opened it, and that user may not settle it. Settling fixes its due
 *  as its row shows it now: the bound figure, else live at the person's grade rate now in force on the claim's date,
 *  at least the sheet's floor, with the late charge as of today frozen in. A clerk (any user not made a steward) may settle
 *  only a claim whose due (as shown: bound by a statement, else live, plus any late charge) is at or below the sheet's clerk limit; a due
 *  of n/a, or no limit, is within it. */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets,
            Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException {
        if (!loaded.status().canBecome(input.to())) {
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        if (input.to() == ClaimStatus.SETTLED && input.user().equals(loaded.openedBy())) {
            throw new ClaimStatusNotAllowedException("The user who opened a claim cannot settle it.");
        }
        if (input.to() == ClaimStatus.SETTLED && Role.CLERK.equals(roles.of(loaded.sheetId(), input.user()))) {
            BigDecimal limit = settings.clerkLimit(loaded.sheetId());
            BigDecimal due = dueNow(loaded, repository, people, rates, settings, sheets);
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                throw new ClaimStatusNotAllowedException("A clerk cannot settle a claim whose due (" + due.toPlainString()
                        + ") is above the clerk limit (" + limit.toPlainString() + ").");
            }
        }
        String openedBy = input.to() == ClaimStatus.OPEN ? input.user() : loaded.openedBy();
        Integer number = switch (input.to()) {
            case OPEN -> repository.nextNumber(loaded.sheetId());
            default -> loaded.number();
        };
        BigDecimal settledDue = input.to() == ClaimStatus.SETTLED ? dueNow(loaded, repository, people, rates, settings, sheets) : null;
        if (!repository.changeStatus(loaded, input.to(), number, settledDue, openedBy)) {
            // the transaction rolls back, so a number taken above is not used up
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), input.to(), number, settledDue, openedBy, loaded.stated(),
                loaded.statedDue(), loaded.corrects())));
    }

    /** The claim's due now, as its row shows it: the figure a statement bound it to, else live (amount times its
     *  person's grade rate in force on the claim's date, divided by 100, rounded to two decimals as the claim rounds, at
     *  least the sheet's floor), with the late charge on it as of the sheet's today added on top; null when it has
     *  no figure. */
    private static BigDecimal dueNow(Claim claim, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) {
        var person = people.find(claim.sheetId(), claim.personId());
        return Dues.shown(claim, person.grades(), rates, settings.floor(claim.sheetId()),
                LateCharge.of(claim.sheetId(), settings, sheets, claims));
    }
}
