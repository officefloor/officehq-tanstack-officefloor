package net.officefloor.hq.app.claim;

/**
 * A claim, as the screen reads it: its amount with two decimals, its date as yyyy-MM-dd and its
 * status, and its number ("" when it has none).
 */
public record ClaimResponse(String id, String personId, String amount, String date, String note,
        String status, String number) {
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(String.valueOf(claim.getId()), String.valueOf(claim.getPersonId()),
                claim.getAmount().toPlainString(), claim.getDate().toString(), claim.getNote(),
                claim.getStatus().name(),
                claim.getNumber() == null ? "" : String.valueOf(claim.getNumber()));
    }
}
