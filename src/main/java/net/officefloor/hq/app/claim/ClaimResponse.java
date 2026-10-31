package net.officefloor.hq.app.claim;

import java.math.RoundingMode;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-mm-dd,
 *  the status as DRAFT, OPEN, PENDING, SETTLED or VOID, the number null when the claim has none, the due
 *  fixed at settling with two decimals, null when it has none (its due is worked out live),
 *  who opened it, null while it has not been opened, whether its person's last statement binds it
 *  and the figure it binds it to (two decimals; null when unbound or bound to n/a), and the number
 *  of the claim a correction corrects (null when it is not a correction), and whether its live
 *  figures round half up (else half to even), by the sheet's policy date. */
public record ClaimResponse(long id, long personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated, String statedDue, String corrects, boolean halfUp) {
    public static ClaimResponse from(Claim claim, Figures figures) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() != null ? claim.getDue().toPlainString() : null, claim.getOpenedBy(),
                claim.isStated(), claim.getStatedDue() != null ? claim.getStatedDue().toPlainString() : null,
                claim.getCorrects(), figures.rounding(claim) == RoundingMode.HALF_UP);
    }
}
