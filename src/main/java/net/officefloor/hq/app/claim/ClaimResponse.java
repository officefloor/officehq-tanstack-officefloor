package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: its amount with two decimals, its date (YYYY-MM-DD) its note, its status, its number (null when it has none)
 * and its due fixed when it was settled (null when it has none). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String due) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getDue() == null ? null : claim.getDue().setScale(2).toPlainString());
    }
}
