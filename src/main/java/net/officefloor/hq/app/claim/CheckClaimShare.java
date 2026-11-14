package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses (400) a share of a SETTLED, VOID or DELETED claim, one given to an unknown person, the claim's own person or
 * a person already holding a share of it, or one that takes the claim's shares above 100.00 in all.
 */
public class CheckClaimShare {
    public void check(@Val Claim claim, @Val ClaimShare share, PersonRepository people, ClaimShareRepository shares)
            throws InvalidClaimException {
        ClaimStatus status = claim.status();
        if (status == ClaimStatus.SETTLED || status == ClaimStatus.VOID || status == ClaimStatus.DELETED) {
            throw new InvalidClaimException("A " + status + " claim cannot be shared.");
        }
        if (people.find(claim.sheetId(), share.personId()) == null) {
            throw new InvalidClaimException("There is no such person on the sheet.");
        }
        if (share.personId() == claim.personId()) {
            throw new InvalidClaimException("A claim cannot be shared with its own person.");
        }
        List<ClaimShare> held = shares.find(claim.sheetId(), claim.id());
        if (held.stream().anyMatch((s) -> s.personId() == share.personId())) {
            throw new InvalidClaimException("That person already holds a share of the claim.");
        }
        BigDecimal total = held.stream().map(ClaimShare::share).reduce(share.share(), BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidClaimException("A claim's shares add up to at most 100.00.");
        }
    }
}
