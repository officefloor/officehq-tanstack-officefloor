package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLogLine;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.person.Person;

/**
 * A period of a sheet: its name (unique on the sheet) and its from and to dates, both inclusive. A closed
 * period keeps the due-total fixed when it was closed ({@code closedTotal}; null while open) and each person's
 * due-total for it then ({@code personTotals}, by person id; empty while open), with the figure carried into it then
 * ({@code personCarried}, by person id; empty while open). A soft-closed period
 * ({@code softClosed}; never also {@code closed}) keeps its figures fixed the same way, but claims due by a date in it can
 * still be added and changed. {@code cap} is the most OPEN or PENDING claims due by a date in it a person may have
 * when another of theirs is opened (null: no cap). A {@code locked} period, locked by a steward, cannot be reopened.
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals, boolean softClosed, Integer cap,
        Map<Long, BigDecimal> personCarried, boolean locked) {

    /** A new, open period with no cap. */
    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, null);
    }

    /** A new, open period with this cap (null: none). */
    public Period(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        this(sheetId, name, from, to, false, null, Map.of(), false, cap, Map.of(), false);
    }

    /** Whether the period's figures are fixed: it is closed or soft-closed. */
    public boolean fixed() {
        return closed || softClosed;
    }

    /** Whether the period shares a day with the other. */
    public boolean overlaps(Period other) {
        return !from.isAfter(other.to()) && !other.from().isAfter(to);
    }

    /** Whether the date falls in the period, both ends included. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /**
     * The status the claim had at the end of the period's last day, as its status log (oldest first) records it: the
     * last line logged on or before it; null when it was added after. A claim with no log is taken at its status now.
     */
    public ClaimStatus statusAtEnd(Claim claim, List<ClaimLogLine> log) {
        if (log.isEmpty()) {
            return claim.status();
        }
        ClaimStatus status = null;
        for (ClaimLogLine line : log) {
            if (!line.date().isAfter(to)) {
                status = line.status();
            }
        }
        return status;
    }

    /**
     * Whether the claim counts toward the period: due by a date in it and OPEN, PENDING or SETTLED at the period's end, left
     * out as the live rules say while it is deferred on today, or when it was open then, is open still, its
     * person's retirement date is reached and no statement binds it (a hold, theirs or the claim's, leaves nothing out). A settled claim voided since leaves it.
     */
    public boolean counts(Claim claim, List<ClaimLogLine> log, Person person, LocalDate today) {
        if (!contains(claim.dueBy())) {
            return false;
        }
        ClaimStatus then = statusAtEnd(claim, log);
        if (then == ClaimStatus.SETTLED) {
            return claim.status() != ClaimStatus.VOID;
        }
        if (then == null || !then.open() || claim.deferred(today)) {
            return false;
        }
        return !claim.status().open() || claim.bound() || !person.heldOn(claim.date(), today);
    }

    /**
     * Whether the claim is carried into the period: due by a date in an earlier period (one that ends before this one
     * starts), still OPEN or PENDING at this period's end, and counted as the period figures count it.
     */
    public boolean carries(Claim claim, List<ClaimLogLine> log, Person person, LocalDate today, List<Period> periods) {
        Period own = periods.stream().filter((p) -> p.contains(claim.dueBy())).findFirst().orElse(null);
        if (own == null || !own.to().isBefore(from)) {
            return false;
        }
        ClaimStatus then = statusAtEnd(claim, log);
        if (then == null || !then.open() || claim.deferred(today)) {
            return false;
        }
        return !claim.status().open() || claim.bound() || !person.heldOn(claim.date(), today);
    }

    /** The same period, closed with its due-total and each person's due-total and carried figure fixed. */
    public Period close(BigDecimal total, Map<Long, BigDecimal> perPerson, Map<Long, BigDecimal> carried) {
        return new Period(sheetId, name, from, to, true, total, Map.copyOf(perPerson), false, cap, Map.copyOf(carried), locked);
    }

    /** The same period, soft-closed with its due-total and each person's due-total and carried figure fixed. */
    public Period softClose(BigDecimal total, Map<Long, BigDecimal> perPerson, Map<Long, BigDecimal> carried) {
        return new Period(sheetId, name, from, to, false, total, Map.copyOf(perPerson), true, cap, Map.copyOf(carried), locked);
    }

    /** The same period with these fixed per-person due-totals and carried figures. */
    public Period withPersonTotals(Map<Long, BigDecimal> perPerson, Map<Long, BigDecimal> carried) {
        return new Period(sheetId, name, from, to, closed, closedTotal, Map.copyOf(perPerson), softClosed, cap,
                Map.copyOf(carried), locked);
    }

    /** The same period, open again with its due-total and carried figures live. */
    public Period reopen() {
        return new Period(sheetId, name, from, to, false, null, Map.of(), false, cap, Map.of(), locked);
    }

    /** The same period, locked (true) or unlocked (false). */
    public Period withLocked(boolean locked) {
        return new Period(sheetId, name, from, to, closed, closedTotal, personTotals, softClosed, cap, personCarried,
                locked);
    }
}
