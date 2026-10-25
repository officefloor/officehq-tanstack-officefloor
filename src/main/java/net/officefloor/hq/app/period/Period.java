package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A period of a sheet: its name and its from and to dates, both inclusive; whether it is closed, and the due-total
 * fixed when it was closed (null while open), and
 * each person's due-total for it fixed then, by person id (empty while open).
 */
public record Period(long id, String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, Map<Long, BigDecimal> personTotals) {
}
