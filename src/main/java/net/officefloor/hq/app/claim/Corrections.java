package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/**
 * A claim with a negative amount is a correction: allowed only when it names, by number, a SETTLED claim of the same
 * person that it corrects, and only up to what remains of that claim: its amount less the amounts of the corrections
 * already made against it (VOID or DELETED ones left out). Any other claim corrects nothing (a number given with it is not
 * kept).
 */
public final class Corrections {

    private Corrections() {
    }

    /** The number the claim with these values corrects (null when it is not a correction); a correction that does
     *  not name a SETTLED claim of the person is refused. */
    public static String check(String sheetId, long personId, ClaimValues values, ClaimRepository claims)
            throws InvalidClaimException {
        return check(sheetId, personId, values, null, claims);
    }

    /** As check, for the claim {@code self} being edited (null for an add), whose own amount does not count against
     *  what remains of the claim it corrects; a correction larger than what remains is refused. */
    public static String check(String sheetId, long personId, ClaimValues values, Long self, ClaimRepository claims)
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
        if (remaining(corrected, self, claims).add(values.amount()).signum() < 0) {
            throw new InvalidClaimException("A correction may not exceed what remains of the claim it corrects");
        }
        return values.corrects();
    }

    /** The claim's amount less the amounts of the corrections made against it (VOID or DELETED ones, and {@code self},
     *  left out). */
    private static BigDecimal remaining(Claim corrected, Long self, ClaimRepository claims) {
        BigDecimal remaining = corrected.amount();
        for (Claim c : claims.list(corrected.sheetId())) {
            if (c.personId() == corrected.personId() && corrected.number().equals(c.corrects())
                    && c.status() != ClaimStatus.VOID && c.status() != ClaimStatus.DELETED
                    && (self == null || c.id() != self)) {
                remaining = remaining.add(c.amount());
            }
        }
        return remaining;
    }
}
