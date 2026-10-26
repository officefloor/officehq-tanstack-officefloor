package net.officefloor.hq.app.claim;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-MM-dd. */
public record ClaimResponse(long id, long personId, String amount, String date, String note) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note());
    }
}
