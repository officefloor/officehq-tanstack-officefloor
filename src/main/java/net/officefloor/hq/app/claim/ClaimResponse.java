package net.officefloor.hq.app.claim;

/** A claim as the screen shows it: the amount with two decimals, the date as yyyy-mm-dd, the status, its
 *  number (null when it has none) and the due fixed when it was settled (two decimals; null when it is not
 *  settled, or its grade had no rate then, so its due follows the rates), who opened it (null while a draft),
 *  whether its person's last statement bound it and the figure it bound it to (two decimals; null when not
 *  bound, or n/a then), and the number a correction corrects (null when it is not one). */
public record ClaimResponse(long id, long personId, String amount, String date, String note, String status,
        String number, String settledDue, String openedBy, boolean stated, String statedDue, String corrects) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.id(), c.personId(), c.amount().toPlainString(), c.date().toString(), c.note(),
                c.status().name(), c.number(), c.settledDue() != null ? c.settledDue().toPlainString() : null,
                c.openedBy(), c.stated(), c.statedDue() != null ? c.statedDue().toPlainString() : null,
                c.corrects());
    }
}
