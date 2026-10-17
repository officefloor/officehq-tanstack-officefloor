package net.officefloor.hq.app.claim;

/** A claim as shown: its amount with two decimals, its date as yyyy-mm-dd and its status. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name());
    }
}
