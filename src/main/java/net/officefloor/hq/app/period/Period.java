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
 * due-total for it then ({@code personTotals}, by person id; empty while open).
 */
public record Period(String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals) {

    /** A new, open period. */
    public Period(String sheetId, String name, LocalDate from, LocalDate to) {
        this(sheetId, name, from, to, false, null, Map.of());
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
     * Whether the claim counts toward the period: dated in it and OPEN, PENDING or SETTLED at the period's end, left
     * out as the live rules say while it is deferred on today, or when it was open then, is open still, a hold of its
     * person covers it and no statement binds it (a hold leaves out only open claims). A settled claim voided since
     * leaves it.
     */
    public boolean counts(Claim claim, List<ClaimLogLine> log, Person person, LocalDate today) {
        if (!contains(claim.date())) {
            return false;
        }
        ClaimStatus then = statusAtEnd(claim, log);
        if (then == ClaimStatus.SETTLED) {
            return claim.status() != ClaimStatus.VOID;
        }
        if (then == null || !then.open() || claim.deferred(today)) {
            return false;
        }
        return !claim.status().open() || claim.bound() || person.holds().stream().noneMatch((h) -> h.covers(claim.date()));
    }

    /** The same period, closed with its due-total and each person's due-total fixed. */
    public Period close(BigDecimal total, Map<Long, BigDecimal> perPerson) {
        return new Period(sheetId, name, from, to, true, total, Map.copyOf(perPerson));
    }

    /** The same period with these fixed per-person due-totals. */
    public Period withPersonTotals(Map<Long, BigDecimal> perPerson) {
        return new Period(sheetId, name, from, to, closed, closedTotal, Map.copyOf(perPerson));
    }

    /** The same period, open again with its due-total live. */
    public Period reopen() {
        return new Period(sheetId, name, from, to, false, null, Map.of());
    }
}
