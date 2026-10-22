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
 *  at least the sheet's floor, with the late charge as of today frozen in. When a clerk (any user not made a steward)
 *  settles a claim whose due (as shown: bound by a statement, else live, plus any late charge) is above the sheet's
 *  clerk limit, it becomes PENDING instead (a due of n/a, or no limit, is within it). A steward other than the
 *  user who opened it approves a PENDING claim (it becomes SETTLED, its due as its row shows it then — live, a
 *  PENDING claim is not bound by a statement — frozen in) or rejects it (OPEN again, bound again by the statement
 *  that bound it). */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets,
            Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException {
        ClaimStatus to = input.to();
        boolean review = input.action().equals("approve") || input.action().equals("reject");
        if (review) {
            if (loaded.status() != ClaimStatus.PENDING) {
                throw new ClaimStatusNotAllowedException("Only a PENDING claim can be " + input.action()
                        + "d; this one is " + loaded.status() + ".");
            }
            if (!Role.STEWARD.equals(roles.of(loaded.sheetId(), input.user()))) {
                throw new ClaimStatusNotAllowedException("Only a steward can " + input.action() + " a PENDING claim.");
            }
            if (input.action().equals("approve") && input.user().equals(loaded.openedBy())) {
                throw new ClaimStatusNotAllowedException("The user who opened a claim cannot approve it.");
            }
        } else if (!loaded.status().canBecome(to)) {
            throw new ClaimStatusNotAllowedException(loaded.status(), to);
        }
        if (input.action().equals("settle") && input.user().equals(loaded.openedBy())) {
            throw new ClaimStatusNotAllowedException("The user who opened a claim cannot settle it.");
        }
        if (input.action().equals("settle") && Role.CLERK.equals(roles.of(loaded.sheetId(), input.user()))) {
            BigDecimal limit = settings.clerkLimit(loaded.sheetId());
            BigDecimal due = dueNow(loaded, repository, people, rates, settings, sheets);
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                to = ClaimStatus.PENDING;
            }
        }
        boolean opening = input.action().equals("open");
        String openedBy = opening ? input.user() : loaded.openedBy();
        String number = opening ? repository.nextNumber(loaded.sheetId(), loaded.date()) : loaded.number();
        BigDecimal settledDue = to == ClaimStatus.SETTLED ? dueNow(loaded, repository, people, rates, settings, sheets) : null;
        if (!repository.changeStatus(loaded, to, number, settledDue, openedBy)) {
            // the transaction rolls back, so a number taken above is not used up
            throw new ClaimStatusNotAllowedException(loaded.status(), to);
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), to, number, settledDue, openedBy, loaded.stated(),
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
