package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-MM-dd, the status by name. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name());
    }
}
