package net.officefloor.hq.app.claim;

import java.util.List;

/** amount is shown with two decimals. */
public record ClaimResponse(String id, String personId, String amount, String date, String note) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().setScale(2).toPlainString(), claim.getDate().toString(), claim.getNote());
    }

    public static List<ClaimResponse> from(List<Claim> claims) {
        return claims.stream().map(ClaimResponse::from).toList();
    }
}
