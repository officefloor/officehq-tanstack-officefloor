package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-MM-dd, the status by name,
 *  the number (null when it has none), the due fixed when it was settled (null while it is live),
 *  who opened it (null until it is opened). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String settledDue, String openedBy) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getSettledDue() == null ? null : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy());
    }
}
