package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Move the loaded claim to its new status, refusing a change its status does not allow (or one
 *  another user's change got to first). Opening gives the claim the sheet's next number; voiding
 *  clears it. Settling fixes its due at the person's grade rate as it is now. */
public class SaveClaimStatus {
    public void save(@Val ClaimStatusInput input, @Val Claim loaded, ClaimRepository repository,
            PersonRepository people, RateRepository rates, Out<ClaimResponse> saved)
            throws ClaimStatusNotAllowedException {
        if (!loaded.status().canBecome(input.to())) {
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        Integer number = switch (input.to()) {
            case OPEN -> repository.nextNumber(loaded.sheetId());
            case VOID -> null;
            default -> loaded.number();
        };
        BigDecimal settledDue = input.to() == ClaimStatus.SETTLED ? dueNow(loaded, people, rates) : null;
        if (!repository.changeStatus(loaded, input.to(), number, settledDue)) {
            // the transaction rolls back, so a number taken above is not used up
            throw new ClaimStatusNotAllowedException(loaded.status(), input.to());
        }
        saved.set(ClaimResponse.from(new Claim(loaded.id(), loaded.sheetId(), loaded.personId(), loaded.amount(),
                loaded.date(), loaded.note(), input.to(), number, settledDue)));
    }

    /** The claim's due now: amount times its person's grade rate, divided by 100, rounded half up to two
     *  decimals; null when the grade has no rate. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates) {
        var person = people.find(claim.sheetId(), claim.personId());
        return rates.list(claim.sheetId()).stream().filter(r -> r.grade().equals(person.grade())).findFirst()
                .map(r -> claim.amount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(null);
    }
}
