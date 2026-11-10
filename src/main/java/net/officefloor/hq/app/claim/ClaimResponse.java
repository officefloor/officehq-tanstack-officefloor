package net.officefloor.hq.app.claim;

/** A claim, as the screen reads it: its amount with two decimals and its date as yyyy-MM-dd. */
public record ClaimResponse(String id, String personId, String amount, String date, String note) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().toPlainString(), claim.getDate().toString(), claim.getNote());
    }
}
