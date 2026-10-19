package net.officefloor.hq.app.claim;

import java.util.List;

/** A claim with a negative amount is a correction: it is allowed only when it names, by number, a
 *  SETTLED claim of the same person. Its due is its amount times the rate, rounded by the date of the
 *  claim it corrects, with no floor and no late charge. */
final class Correction {

    private Correction() {
    }

    /** Refuses a correction whose number is not a SETTLED claim of the person; any other claim passes. */
    static void refuse(ValidClaim request, String sheetId, long personId, ClaimRepository claims)
            throws InvalidClaimException {
        if (request.corrects() == null) return;
        List<Claim> theirs = claims.findByPerson(sheetId, personId);
        boolean settled = theirs.stream().anyMatch(c -> request.corrects().equals(c.getNumber())
                && c.getStatus() == ClaimStatus.SETTLED);
        if (!settled) {
            throw new InvalidClaimException("Claim " + request.corrects() + " is not a settled claim of this person.");
        }
    }
}
