package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-MM-dd, the status by name,
 * the number null unless it has one, the due fixed at settling (two decimals) or null while it is live,
 * the user who opened it or null while it is a draft, whether a statement bound it and the figure it bound
 * it to (two decimals; null: n/a), and the number of the claim a correction corrects (null: not a correction). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status, String number,
        String due, String openedBy, boolean stated, String statedDue, String corrects) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.due() == null ? null : claim.due().setScale(2).toPlainString(), claim.openedBy(),
                claim.stated(), claim.statedDue() == null ? null : claim.statedDue().setScale(2).toPlainString(),
                claim.corrects());
    }
}
