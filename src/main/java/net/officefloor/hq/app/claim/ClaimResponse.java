package net.officefloor.hq.app.claim;

/** A claim as the front end reads it: the amount with two decimals, the date as yyyy-MM-dd,
 *  the status by name, the number as given when opened (null when it has none), the due fixed when
 *  settled with two decimals (null when it has none, so its due is worked out from the current rate). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status, Long number,
        String due) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.getId(), c.getPersonId(), c.getAmount().toPlainString(), c.getDate().toString(),
                c.getNote(), c.getStatus().name(), c.getNumber(),
                c.getSettledDue() == null ? null : c.getSettledDue().setScale(2).toPlainString());
    }
}
