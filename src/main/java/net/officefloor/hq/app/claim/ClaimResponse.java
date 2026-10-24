package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: its amount with two decimals, its date (YYYY-MM-DD) its note, its status, its number (null when it has none)
 * and its due fixed when it was settled (null when it has none), and who opened it (null when never opened);
 * whether its person's last statement binds it, and the figure it binds it to (null: none, or n/a); the number of the
 * claim it corrects (null when it is not a correction); and the date its rounding goes by (YYYY-MM-DD: a correction's is
 * the date of the claim it corrects, else its own). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String due, String openedBy,
        boolean stated, String statedDue, Integer corrects, String roundsOn) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() == null ? null : claim.getDue().setScale(2).toPlainString(), claim.getOpenedBy(),
                claim.isStated(), claim.getStatedDue() == null ? null : claim.getStatedDue().setScale(2).toPlainString(),
                claim.getCorrects(), claim.getRoundsOn().toString());
    }
}
