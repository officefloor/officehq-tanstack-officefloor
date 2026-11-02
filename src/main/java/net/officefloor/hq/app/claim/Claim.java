package net.officefloor.hq.app.claim;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd), note and status
 *  (DRAFT, OPEN, PENDING — a clerk's settle above the clerk limit, awaiting a steward — SETTLED or VOID), its number on the sheet (a sheet-wide 1, 2, ... or, under period numbering, <period>-<n> or X-<n>; null while a draft; a voided claim keeps the one it had), and the due fixed when it was settled
 *  (null unless settled with a rate; its due is then worked out live from the rates), and who opened it
 *  (null until it is opened), whether its person's last statement bound it, and the figure it was bound to
 *  (null when not bound, or bound with no figure: its due then stays live), and, for a correction (a negative
 *  amount), the number of the settled claim it corrects (null otherwise). */
public record Claim(long id, long personId, String amount, String date, String note, String status,
        String number, String due, String openedBy, boolean stated, String statedDue, String corrects) {

    /** Whether the claim is open: OPEN, or PENDING (handled like OPEN everywhere but its statement binding). */
    public boolean open() {
        return status.equals("OPEN") || status.equals("PENDING");
    }

    /** Whether the claim counts toward claimed and every total: only open (OPEN or PENDING) and SETTLED claims do. */
    public boolean counts() {
        return open() || status.equals("SETTLED");
    }

    /** Whether the claim counts toward live totals (claimed, due-totals, a period's total) when its
     *  person is or is not on hold: a held person's open claims do not; SETTLED claims always do. */
    public boolean counts(boolean held) {
        return status.equals("SETTLED") || (!held && open());
    }

    /** Whether the claim keeps a fixed figure: its settled due, else the one its last statement bound it
     *  to — except while PENDING, when the statement does not bind it and its figure is live. */
    public boolean fixed() {
        return due != null || (bound() && statedDue != null);
    }

    /** Whether its last statement binds it now: it was stated and is not PENDING. */
    public boolean bound() {
        return stated && !status.equals("PENDING");
    }

    /** The audit field a numbered claim adds at the end of its lines: " number=<n>", or "" when it has none. */
    static String auditNumber(String number) {
        return number == null ? "" : " number=" + number;
    }
}
