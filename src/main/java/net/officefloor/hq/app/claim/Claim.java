package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.person.Hold;

/** A claim of a person on a sheet: its amount with two decimals, its date (ISO yyyy-MM-dd), note and status
 *  (DRAFT, OPEN, PENDING — a clerk's settle above the clerk limit, awaiting a steward — SETTLED or VOID), its number on the sheet (a sheet-wide 1, 2, ... or, under period numbering, <period>-<n> or X-<n>; null while a draft; a voided claim keeps the one it had), and the due fixed when it was settled
 *  (null unless settled with a rate; its due is then worked out live from the rates), and who opened it
 *  (null until it is opened), whether its person's last statement bound it, and the figure it was bound to
 *  (null when not bound, or bound with no figure: its due then stays live), and, for a correction (a negative
 *  amount), the number of the settled claim it corrects (null otherwise), and its due-by (yyyy-MM-dd: fixed when it was
 *  created, its date then plus its person's own terms, else the terms of their grade on that date, else the sheet's terms), and
 *  whether its last statement bound it to n/a (the rate that would apply to it had ended: its due then
 *  stays n/a, not live), and the date it is deferred until (null when never deferred) and whether it is
 *  deferred now (OPEN and that date after the sheet's today: it then counts in no total, is not overdue,
 *  cannot be settled and a statement leaves it unbound), and how many proofs it has been given, and, for a claim
 *  split off another, the number of the claim it was split from (null otherwise), and whether it was a late entry
 *  (added while the period its date falls in was soft-closed). */
public record Claim(long id, long personId, String amount, String date, String note, String status,
        String number, String due, String openedBy, boolean stated, String statedDue, String corrects,
        String dueBy, boolean statedNa, String deferredUntil, boolean deferred, int proofs, String splitFrom,
        boolean lateEntry) {

    /** Whether the claim is open: OPEN, or PENDING (handled like OPEN everywhere but its statement binding). */
    public boolean open() {
        return status.equals("OPEN") || status.equals("PENDING");
    }

    /** Whether the claim counts toward claimed and every total: only open (OPEN or PENDING) and SETTLED claims do. */
    public boolean counts() {
        return open() || status.equals("SETTLED");
    }

    /** Whether the claim counts toward live totals (claimed, due-totals, a period's total) given its
     *  person's holds: an open claim dated within a hold does not, unless a statement binds it; SETTLED
     *  claims always do. A deferred claim does not. */
    public boolean counts(List<Hold> holds) {
        return status.equals("SETTLED") || (open() && !deferred && (bound() || !Hold.anyCovers(holds, date)));
    }

    /** Whether the claim counts toward a period ending on a day, given the status it had at the end of that
     *  day (null when it did not exist then: it does not count) and its person's holds: the live rules
     *  applied to that status (an open claim deferred until after the sheet's today does not count either). A hold
     *  leaves out only a claim still open now: one since settled or voided counts by the status it had then. */
    public boolean countsAs(String statusThen, List<Hold> holds, String today) {
        if (statusThen == null) return false;
        boolean deferredNow = statusThen.equals("OPEN") && deferredUntil != null && today != null && today.compareTo(deferredUntil) < 0;
        Claim then = new Claim(id, personId, amount, date, note, statusThen, number, due, openedBy, stated, statedDue,
                corrects, dueBy, statedNa, deferredUntil, deferredNow, proofs, splitFrom, lateEntry);
        return then.counts(open() ? holds : List.of());
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
