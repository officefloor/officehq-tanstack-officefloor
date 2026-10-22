package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-mm-dd. */
public record ClaimResponse(long id, long personId, String amount, String date, String note) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.id(), c.personId(), c.amount().toPlainString(), c.date().toString(), c.note());
    }
}
