package net.officefloor.hq.app.claim;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-mm-dd,
 *  the status as DRAFT, OPEN, SETTLED or VOID. */
public record ClaimResponse(long id, long personId, String amount, String date, String note,
        String status) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name());
    }
}
