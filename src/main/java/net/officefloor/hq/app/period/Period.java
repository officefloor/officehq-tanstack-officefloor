package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A period of a sheet: a name unique on the sheet and its from and to dates, both inclusive. A closed period keeps
 *  the due-total fixed when it was closed; a soft-closed one ({@code soft}, not closed) keeps the due-total fixed when it
 *  was soft-closed, while claims dated inside it can still change. */
public record Period(long id, String sheetId, String name, LocalDate from, LocalDate to, boolean closed,
        BigDecimal closedTotal, boolean soft) {
}
