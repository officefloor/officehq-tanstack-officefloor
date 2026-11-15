package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A claim with a negative amount is a correction: allowed only when it names, by number, a SETTLED
 *  claim of the same person that it corrects, and when it does not exceed what remains of that claim
 *  (its amount less the corrections already made against it, VOID or DELETED ones left out). */
public final class Corrections {
    private Corrections() {
    }

    /** The number a claim corrects: none for a claim that is not a correction; refused when a
     *  correction names no SETTLED claim of the person, or exceeds what remains of it. */
    public static String check(ValidClaim request, String sheetId, long personId, ClaimRepository claims)
            throws InvalidClaimException {
        return check(request, sheetId, personId, claims, 0);
    }

    /** As {@link #check(ValidClaim, String, long, ClaimRepository)}, leaving the claim {@code exceptId}
     *  (the one being edited) out of the corrections already made. */
    public static String check(ValidClaim request, String sheetId, long personId, ClaimRepository claims,
            long exceptId) throws InvalidClaimException {
        if (!request.isCorrection()) return null;
        Claim corrected = claims.findByNumber(sheetId, request.corrects());
        if (corrected == null || corrected.getPersonId() != personId
                || !ClaimStatus.SETTLED.name().equals(corrected.getStatus())) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person");
        }
        BigDecimal remains = new BigDecimal(corrected.getAmount());
        for (Claim c : claims.listOf(sheetId, personId)) {
            if (c.getId() == exceptId || !request.corrects().equals(c.getCorrects())
                    || ClaimStatus.VOID.name().equals(c.getStatus())
                    || ClaimStatus.DELETED.name().equals(c.getStatus())) continue;
            remains = remains.add(new BigDecimal(c.getAmount()));
        }
        if (request.amount().negate().compareTo(remains) > 0) {
            throw new InvalidClaimException("A correction may not exceed what remains of the claim: "
                    + remains.setScale(2).toPlainString());
        }
        return request.corrects();
    }
}
