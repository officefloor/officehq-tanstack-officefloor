package net.officefloor.hq.app.claim;

/**
 * A claim, as the screen reads it: its amount with two decimals, its date as yyyy-MM-dd and its
 * status, its number ("" when it has none), and its due as fixed when it was settled ("" while
 * its due is live), and who opened it ("" until it is opened); whether its person's last
 * statement bound it, and the figure it was bound to ("" when not stated, n/a when it had none);
 * and the number of the claim it corrects ("" when it is no correction); and the date its figures
 * round by (a correction's is the date of the claim it corrects).
 */
public record ClaimResponse(String id, String personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated,
        String statedDue, String corrects, String roundDate) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(),
                claim.getNumber() == null ? "" : String.valueOf(claim.getNumber()),
                claim.getSettledDue() == null ? "" : claim.getSettledDue().toPlainString(),
                claim.getOpenedBy() == null ? "" : claim.getOpenedBy(),
                claim.isStated(),
                !claim.isStated() ? "" : claim.getStatedDue() == null ? "n/a" : claim.getStatedDue().toPlainString(),
                claim.getCorrects() == null ? "" : String.valueOf(claim.getCorrects()),
                claim.getRoundDate().toString());
    }
}
