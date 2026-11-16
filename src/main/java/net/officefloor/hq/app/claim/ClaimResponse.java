package net.officefloor.hq.app.claim;

/** A claim as sent to the screen: the amount as text with two decimals, so it is never rounded through a float. */
public record ClaimResponse(long id, String sheetId, long personId, String amount, String date, String note,
        String status) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.id(), claim.sheetId(), claim.personId(), claim.amount().toPlainString(),
                claim.date().toString(), claim.note(), claim.status().name());
    }
}
