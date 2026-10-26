package net.officefloor.hq.app.claim;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-MM-dd, the status by name. */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name());
    }
}
