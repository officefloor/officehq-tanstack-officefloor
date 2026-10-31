package net.officefloor.hq.app.claim;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-mm-dd,
 *  the status as DRAFT, OPEN, SETTLED or VOID, the number null when the claim has none, the due
 *  fixed at settling with two decimals, null when it has none (its due is worked out live). */
public record ClaimResponse(long id, long personId, String amount, String date, String note,
        String status, Long number, String due) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() != null ? claim.getDue().toPlainString() : null);
    }
}
