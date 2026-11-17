package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Checks a share: a person of the sheet other than the claim's own, not already holding a share of it, a share more
 *  than 0.00 that keeps the claim's shares at most 100.00, and a claim that is not SETTLED, VOID or DELETED. */
public class ApplyShare {
    public void apply(@Val Claim claim, @Val ClaimSharing sharing, PersonRepository people, ClaimRepository claims)
            throws InvalidClaimStatusException {
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID
                || claim.status() == ClaimStatus.DELETED) {
            throw new InvalidClaimStatusException("A " + claim.status() + " claim cannot be shared");
        }
        if (sharing.personId() == null || people.find(claim.sheetId(), sharing.personId()) == null) {
            throw new InvalidClaimStatusException("No such person to share with");
        }
        if (sharing.personId() == claim.personId()) {
            throw new InvalidClaimStatusException("A claim is not shared with its own person");
        }
        if (sharing.share() == null || sharing.share().signum() <= 0) {
            throw new InvalidClaimStatusException("A share is a percent more than 0.00");
        }
        BigDecimal total = sharing.share();
        for (ClaimShare s : claims.shares(claim.sheetId(), claim.id())) {
            if (s.personId() == sharing.personId()) {
                throw new InvalidClaimStatusException("The person already holds a share of the claim");
            }
            total = total.add(s.share());
        }
        if (total.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidClaimStatusException("A claim's shares add up to at most 100.00");
        }
    }
}
