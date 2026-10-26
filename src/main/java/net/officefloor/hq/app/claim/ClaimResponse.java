package net.officefloor.hq.app.claim;

/** A claim as the screen sees it: the amount with two decimals, the date as yyyy-MM-dd, the status by name, the number (null when it has none), the due fixed on settling (null when it has none), who opened it (null when never opened), whether its person's last statement bound it, the figure it bound (null when it has none) and the number a correction corrects (null for any other claim). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Long number, String settledDue, String openedBy, boolean stated, String statedDue,
        Long corrects) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.id(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.settledDue() == null ? null : claim.settledDue().toPlainString(), claim.openedBy(), claim.stated(),
                claim.statedDue() == null ? null : claim.statedDue().toPlainString(), claim.corrects());
    }
}
