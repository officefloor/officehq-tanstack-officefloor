package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number, which it keeps when voided. Settling fixes the claim's
 *  due at the person's grade's rate in force on the claim's date (none when there is none, so it stays live).
 *  A claim a statement binds is settled at its bound figure.
 *  The user who opened a claim may not settle it (four eyes).
 *  Publishes the number the audit line carries. */
public class ApplyClaimAction {
    public void apply(@Val ClaimAction action, @Val Claim claim, ClaimRepository repository,
            PersonRepository people, RateRepository rates, Out<ClaimNumber> numberOut,
            @RequestParam(value = "user", required = false) String user)
            throws InvalidClaimStatusException {
        if (!action.allowedFrom(claim.getStatus())) {
            throw new InvalidClaimStatusException(
                    "A " + claim.getStatus() + " claim cannot be moved to " + action.to() + ".");
        }
        if (action == ClaimAction.SETTLE && claim.getOpenedBy() != null && claim.getOpenedBy().equals(user)) {
            throw new InvalidClaimStatusException("The user who opened a claim cannot settle it.");
        }
        claim.setStatus(action.to());
        switch (action) {
            case OPEN -> {
                claim.setNumber(repository.nextNumber(claim.getSheetId()));
                claim.setOpenedBy(user);
            }
            case SETTLE -> claim.setSettledDue(claim.isStated() ? claim.getStatedDue() : dueNow(claim, people, rates));
            default -> { }
        }
        numberOut.set(new ClaimNumber(claim.getNumber()));
    }

    /** amount × pct / 100, rounded half to even to two decimals; null when no rate is in force on the claim's date. */
    private static BigDecimal dueNow(Claim claim, PersonRepository people, RateRepository rates) {
        Person person = people.findById(claim.getSheetId(), claim.getPersonId());
        return Rate.inForce(rates.findBySheet(claim.getSheetId()), person.getGrade(), claim.getDate())
                .map(r -> claim.getAmount().multiply(r.pct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .orElse(null);
    }
}
