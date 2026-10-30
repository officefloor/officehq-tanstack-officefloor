package net.officefloor.hq.app.claim;

import java.util.List;

/** amount is shown with two decimals; status is DRAFT, OPEN, SETTLED or VOID; number is "" when none;
 *  due is the due fixed when the claim was settled, else the figure its person's last statement bound
 *  it to, "" while it is live (or bound to n/a); openedBy is "" when never opened; stated is whether
 *  the last statement bound it; corrects is the number a correction corrects, "" for any other claim;
 *  roundingDate is the date its rounding goes by (a correction's: the date of the claim it corrects). */
public record ClaimResponse(String id, String personId, String amount, String date, String note, String status,
        String number, String due, String openedBy, boolean stated, String corrects,
        String roundingDate) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().setScale(2).toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(), claim.getNumber() == null ? "" : claim.getNumber(),
                fixedDue(claim) == null ? "" : fixedDue(claim).setScale(2).toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy(), claim.isStated(),
                claim.getCorrects() == null ? "" : claim.getCorrects(),
                claim.getRoundingDate().toString());
    }

    /** The due a claim keeps whatever the rates do: fixed when settled, else bound by a statement. */
    public static java.math.BigDecimal fixedDue(Claim claim) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        return claim.isStated() ? claim.getStatedDue() : null;
    }

    public static List<ClaimResponse> from(List<Claim> claims) {
        return claims.stream().map(ClaimResponse::from).toList();
    }
}
