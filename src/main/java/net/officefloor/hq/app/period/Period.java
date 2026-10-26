package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A period of a sheet: its name and its from and to dates, both inclusive; whether it is closed, and the due-total
 * fixed when it was closed (null while open), and
 * each person's due-total for it fixed then, by person id (empty while open); and whether it is soft-closed: its
 * figures fixed as a close fixes them, but not closed, so claims dated inside it can still change; and the cap on each person's OPEN or PENDING claims in it
 * (null for none); and each person's carried figure for it fixed when it was closed or soft-closed, by person id (empty while open); and whether
 * a steward has locked it, so it cannot be reopened.
 */
public record Period(long id, String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals, boolean soft,
        Integer cap, Map<Long, BigDecimal> personCarried, boolean locked) {
}
