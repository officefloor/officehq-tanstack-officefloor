package net.officefloor.hq.app.claim;

/**
 * A claim with a negative amount is a correction: allowed only when it names, by number, a SETTLED claim of the same
 * person that it corrects. Any other claim corrects nothing (a number given with it is not kept).
 */
public final class Corrections {

    private Corrections() {
    }

    /** The number the claim with these values corrects (null when it is not a correction); a correction that does
     *  not name a SETTLED claim of the person is refused. */
    public static String check(String sheetId, long personId, ClaimValues values, ClaimRepository claims)
            throws InvalidClaimException {
        if (values.amount().signum() >= 0) {
            return null;
        }
        if (values.corrects() == null) {
            throw new InvalidClaimException("A negative amount must name the settled claim it corrects");
        }
        Claim corrected = claims.findByNumber(sheetId, personId, values.corrects());
        if (corrected == null || corrected.status() != ClaimStatus.SETTLED) {
            throw new InvalidClaimException("A correction must name a settled claim of the same person");
        }
        return values.corrects();
    }
}
