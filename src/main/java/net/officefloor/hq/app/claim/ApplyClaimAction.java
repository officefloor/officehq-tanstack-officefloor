package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number; voiding clears it. Settling fixes the claim's
 *  due at the person's grade's current rate (none when the grade has no rate, so it stays live).
 *  Publishes the number the audit line carries (for a void, the number the claim had). */
public class ApplyClaimAction {
    public void apply(@Val ClaimAction action, @Val Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, Out<ClaimNumber> numberOut)
            throws InvalidClaimStatusException {
        if (!action.allowedFrom(claim.getStatus())) {
            throw new InvalidClaimStatusException(
                    "A " + claim.getStatus() + " claim cannot be moved to " + action.to() + ".");
        }
        Integer had = claim.getNumber();
        claim.setStatus(action.to());
        switch (action) {
            case OPEN -> claim.setNumber(repository.nextNumber(claim.getSheetId()));
            case VOID -> claim.setNumber(null);
            case SETTLE -> claim.setSettledDue(dueNow(claim, people, rates));
            default -> { }
        }
        numberOut.set(new ClaimNumber(action == ClaimAction.VOID ? had : claim.getNumber()));
    }

    /** amount × pct / 100, rounded half up to two decimals; null when the grade has no rate. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates) {
        Person person = people.findById(claim.getSheetId(), claim.getPersonId());
        return rates.findBySheet(claim.getSheetId()).stream()
                .filter(r -> r.grade().equals(person.getGrade()))
                .findFirst()
                .map(r -> claim.getAmount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(null);
    }
}
