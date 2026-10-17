package net.officefloor.hq.app.claim;

/** A claim as shown: its amount with two decimals, its date as yyyy-mm-dd, its status, its number
 *  (null when it has none) and, once settled, its fixed due with two decimals (null while live, or when
 *  no rate applied at settling). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String settledDue) {

    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getPersonId(), claim.getAmount().setScale(2).toPlainString(),
                claim.getDate().toString(), claim.getNote(), claim.getStatus().name(), claim.getNumber(),
                claim.getSettledDue() == null ? null : claim.getSettledDue().setScale(2).toPlainString());
    }
}
