package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new status: DRAFT to OPEN, OPEN to SETTLED, DRAFT or OPEN to VOID. Voiding clears the
 * claim's number; the number it had is kept for the audit line. Settling fixes the claim's due: its amount at its
 * person's grade's rate in force on the claim's date, rounded half up to two decimals (left live when no rate applies).
 */
public class ApplyClaimStatus {
    public void apply(@Val Claim claim, @Val ClaimStatusChange change, PersonRepository people, RateRepository rates,
            Out<Claim> changed, Out<ClaimNumber> number) throws InvalidClaimStatusException {
        boolean allowed = switch (change.to()) {
            case OPEN -> claim.status() == ClaimStatus.DRAFT;
            case SETTLED -> claim.status() == ClaimStatus.OPEN;
            case VOID -> claim.status() == ClaimStatus.DRAFT || claim.status() == ClaimStatus.OPEN;
            case DRAFT -> false;
        };
        if (!allowed) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be made " + change.to());
        }
        number.set(new ClaimNumber(claim.number()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), change.to(), change.to() == ClaimStatus.VOID ? null : claim.number(),
                change.to() == ClaimStatus.SETTLED ? due(claim, people, rates) : claim.due()));
    }

    private static BigDecimal due(Claim claim, PersonRepository people, RateRepository rates) {
        Person person = people.find(claim.sheetId(), claim.personId());
        Rate rate = person == null ? null : rates.find(claim.sheetId(), person.grade(), claim.date());
        return rate == null ? null
                : claim.amount().multiply(rate.rate()).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
