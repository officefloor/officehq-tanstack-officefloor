package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the claim to its new status: DRAFT to OPEN, OPEN to SETTLED, DRAFT or OPEN to VOID. Voiding clears the
 * claim's number; the number it had is kept for the audit line. Settling fixes the claim's due: its amount at its
 * person's grade's rate in force on the claim's date, rounded half to even to two decimals (left live when no rate applies),
 * or the figure a statement bound it to.
 * Opening records who opened the claim; that user cannot settle it (four eyes).
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
        if (change.to() == ClaimStatus.SETTLED && change.by().equals(claim.openedBy())) {
            throw new InvalidClaimStatusException("The user who opened a claim cannot settle it");
        }
        number.set(new ClaimNumber(claim.number()));
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), claim.amount(), claim.date(),
                claim.note(), change.to(), change.to() == ClaimStatus.VOID ? null : claim.number(),
                change.to() == ClaimStatus.SETTLED ? due(claim, people, rates) : claim.due(),
                change.to() == ClaimStatus.OPEN ? change.by() : claim.openedBy(), claim.stated(), claim.statedDue()));
    }

    private static BigDecimal due(Claim claim, PersonRepository people, RateRepository rates) {
        if (claim.stated()) {
            return claim.statedDue();
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        return person == null ? null : ClaimDue.live(claim, person.grade(), rates);
    }
}
