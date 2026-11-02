package net.officefloor.hq.app.claim;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd), note and status
 *  (DRAFT, OPEN, SETTLED or VOID), its number on the sheet (null while a draft; a voided claim keeps the one it had), and the due fixed when it was settled
 *  (null unless settled with a rate; its due is then worked out live from the rates), and who opened it
 *  (null until it is opened), whether its person's last statement bound it, and the figure it was bound to
 *  (null when not bound, or bound with no figure: its due then stays live). */
public record Claim(long id, long personId, String amount, String date, String note, String status,
        Long number, String due, String openedBy, boolean stated, String statedDue) {

    /** Whether the claim counts toward claimed and every total: only OPEN and SETTLED claims do. */
    public boolean counts() {
        return status.equals("OPEN") || status.equals("SETTLED");
    }

    /** Whether the claim counts toward live totals (claimed, due-totals, a period's total) when its
     *  person is or is not on hold: a held person's OPEN claims do not; SETTLED claims always do. */
    public boolean counts(boolean held) {
        return status.equals("SETTLED") || (!held && status.equals("OPEN"));
    }

    /** The audit field a numbered claim adds at the end of its lines: " number=<n>", or "" when it has none. */
    static String auditNumber(Long number) {
        return number == null ? "" : " number=" + number;
    }
}
