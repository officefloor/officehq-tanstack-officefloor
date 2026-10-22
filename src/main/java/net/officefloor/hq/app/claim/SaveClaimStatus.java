package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to its new status, refusing a change its status does not allow (or one
 *  another user's change got to first). Opening gives the claim the sheet's next number; voiding
 *  keeps it (a number is never reused). Opening records who opened it, and that user may not settle it. Settling fixes its due
 *  at the person's grade rate now in force on the claim's date. A clerk (any user not made a steward) may settle
 *  only a claim whose due (as shown: bound by a statement, else live) is at or below the sheet's clerk limit; a due
 *  of n/a, or no limit, is within it. */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
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
            BigDecimal due = loaded.statedDue() != null ? loaded.statedDue() : dueNow(loaded, people, rates);
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
        BigDecimal settledDue = input.to() == ClaimStatus.SETTLED ? dueNow(loaded, people, rates) : null;
        if (!repository.changeStatus(loaded, input.to(), number, settledDue, openedBy)) {
            // the transaction rolls back, so a number taken above is not used up
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), input.to(), number, settledDue, openedBy, loaded.stated(),
                loaded.statedDue())));
    }

    /** The claim's due now: amount times its person's grade rate in force on the claim's date, divided by
     *  100, rounded half to even to two decimals; null when no rate of the grade applies on that date. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates) {
        var person = people.find(claim.sheetId(), claim.personId());
        return rates.on(claim.sheetId(), person.grade(), claim.date())
                .map(r -> claim.amount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .orElse(null);
    }
}
