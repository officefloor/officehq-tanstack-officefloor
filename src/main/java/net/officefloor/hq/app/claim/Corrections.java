package net.officefloor.hq.app.claim;

/** A claim with a negative amount is a correction: allowed only when it names, by number, a SETTLED
 *  claim of the same person that it corrects. */
public final class Corrections {
    private Corrections() {
    }

    /** The number a claim corrects: none for a claim that is not a correction; refused when a
     *  correction names no SETTLED claim of the person. */
    public static String check(ValidClaim request, String sheetId, long personId, ClaimRepository claims)
            throws InvalidClaimException {
        if (!request.isCorrection()) return null;
        Claim corrected = claims.findByNumber(sheetId, request.corrects());
        if (corrected == null || corrected.getPersonId() != personId
                || !ClaimStatus.SETTLED.name().equals(corrected.getStatus())) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person");
        }
        return request.corrects();
    }
}
