package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-mm-dd, the status and
 *  its number (null when it has none). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.id(), c.personId(), c.amount().toPlainString(), c.date().toString(), c.note(),
                c.status().name(), c.number());
    }
}
