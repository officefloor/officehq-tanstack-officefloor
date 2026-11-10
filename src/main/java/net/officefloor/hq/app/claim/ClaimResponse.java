package net.officefloor.hq.app.claim;

/**
 * A claim, as the screen reads it: its amount with two decimals, its date as yyyy-MM-dd and its
 * status, its number ("" when it has none), and its due as fixed when it was settled ("" while
 * its due is live), and who opened it ("" until it is opened).
 */
public record ClaimResponse(String id, String personId, String amount, String date, String note,
        String status, String number, String due, String openedBy) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(),
                claim.getNumber() == null ? "" : String.valueOf(claim.getNumber()),
                claim.getSettledDue() == null ? "" : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy());
    }
}
