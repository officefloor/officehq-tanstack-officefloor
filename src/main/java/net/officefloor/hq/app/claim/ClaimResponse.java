package net.officefloor.hq.app.claim;

/** A claim as sent to the screen: the amount as text with two decimals, so it is never rounded through a float;
 *  the number is null when the claim has none; due is the due fixed when settled (two decimals), null while it is
 *  live; openedBy is who opened it, null when never opened; stated is whether it is bound to the figure statedDue
 *  (two decimals, null when it had none) by its person's last statement; corrects is the number of the claim a
 *  correction corrects (null for any other claim); roundsOn is the date that decides how its live figures round
 *  (the corrected claim's date for a correction, else its own; see Rounding). */
public record ClaimResponse(long id, String sheetId, long personId, String amount, String date, String note,
        String status, String number, String due, String openedBy, boolean stated, String statedDue, String corrects,
        String roundsOn) {
    public static ClaimResponse from(Claim claim, ClaimRepository claims) {
        return new ClaimResponse(claim.id(), claim.sheetId(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name(), claim.number(),
                claim.due() == null ? null : claim.due().toPlainString(), claim.openedBy(), claim.stated(),
                claim.statedDue() == null ? null : claim.statedDue().toPlainString(), claim.corrects(),
                Rounding.roundsOn(claim, claims).toString());
    }
}
