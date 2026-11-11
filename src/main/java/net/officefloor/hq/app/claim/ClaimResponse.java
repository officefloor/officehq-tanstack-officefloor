package net.officefloor.hq.app.claim;

/** A claim as the front end reads it: the amount with two decimals, the date as yyyy-MM-dd. */
public record ClaimResponse(long id, long personId, String amount, String date, String note) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.getId(), c.getPersonId(), c.getAmount().toPlainString(), c.getDate().toString(),
                c.getNote());
    }
}
