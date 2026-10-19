package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-MM-dd, the status by name,
 *  the number (null when it has none), the due fixed when it was settled (null while it is live),
 *  who opened it (null until it is opened), whether a statement binds it and the figure it binds it to
 *  (null when unbound or n/a), and the number a correction corrects (null for any other claim). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getSettledDue() == null ? null : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy(), claim.isStated(),
                claim.getStatedDue() == null ? null : claim.getStatedDue().toPlainString(), claim.getCorrects());
    }
}
