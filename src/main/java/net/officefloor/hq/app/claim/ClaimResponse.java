package net.officefloor.hq.app.claim;

import java.util.List;

/** amount is shown with two decimals; status is DRAFT, OPEN, SETTLED or VOID; number is "" when none;
 *  due is the due fixed when the claim was settled, "" while it is live; openedBy is "" when never opened. */
public record ClaimResponse(String id, String personId, String amount, String date, String note, String status,
        String number, String due, String openedBy) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().setScale(2).toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(), claim.getNumber() == null ? "" : String.valueOf(claim.getNumber()),
                claim.getSettledDue() == null ? "" : claim.getSettledDue().setScale(2).toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy());
    }

    public static List<ClaimResponse> from(List<Claim> claims) {
        return claims.stream().map(ClaimResponse::from).toList();
    }
}
