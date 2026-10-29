package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/**
 * The check, shared by an add and an edit, that a correction names a SETTLED claim of the same person and takes back
 * no more than remains of it: its amount less the corrections already made against it (VOID or DELETED ones left out).
 */
final class RefuseInvalidCorrection {

    private RefuseInvalidCorrection() {
    }

    static void refuse(ClaimInput input, String sheetId, long personId, Claims claims) throws InvalidClaimException {
        refuse(input, sheetId, personId, 0, claims);
    }

    /** As {@link #refuse(ClaimInput, String, long, Claims)}, leaving the claim {@code exceptId} (0: none) out of the corrections already made. */
    static void refuse(ClaimInput input, String sheetId, long personId, long exceptId, Claims claims)
            throws InvalidClaimException {
        if (input.corrects() == null) {
            return;
        }
        Claim corrected = claims.findByNumber(sheetId, personId, input.corrects());
        if (corrected == null || corrected.status() != ClaimStatus.SETTLED) {
            throw new InvalidClaimException(
                    "A correction must name a settled claim of the same person: " + input.corrects() + " is not one.");
        }
        BigDecimal remains = corrected.amount().subtract(claims.corrected(sheetId, personId, input.corrects(), exceptId));
        if (input.amount().negate().compareTo(remains) > 0) {
            throw new InvalidClaimException("A correction of " + input.corrects() + " may take back at most "
                    + remains.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + ".");
        }
    }
}
