package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-mm-dd, the status, its
 *  number (null when it has none) and the due fixed when it was settled (two decimals; null when it is not
 *  settled, or its grade had no rate then, so its due follows the rates) and who opened it (null while a draft). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        Integer number, String settledDue, String openedBy) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.id(), c.personId(), c.amount().toPlainString(), c.date().toString(), c.note(),
                c.status().name(), c.number(), c.settledDue() != null ? c.settledDue().toPlainString() : null,
                c.openedBy());
    }
}
