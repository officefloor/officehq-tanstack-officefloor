package net.officefloor.hq.app.history;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Reads a table as it stood at the end of a date, from the versions {@link HistoryTrigger} keeps: each row's
 *  last version made while the sheet's today was on or before the date, unless that version deleted it. A
 *  change made while the sheet's today was after the date does not count, whenever it was made. */
public final class History {
    private History() {
    }

    /** The date of an {@code asOf} query parameter (yyyy-MM-dd): null when absent or blank, so the store is
     *  read as it is now. */
    public static LocalDate date(String asOf) {
        return asOf == null || asOf.isBlank() ? null : LocalDate.parse(asOf.trim());
    }

    /** A table expression, named as the table, for use in place of it in a query's FROM: the table's rows
     *  (each identified by {@code keys}, comma separated) as they stood at the end of {@code date}. */
    public static String asOf(String table, String keys, LocalDate date) {
        String on = "DATE '" + date + "'";
        String same = Arrays.stream(keys.split(",")).map(String::trim).map(k -> "l." + k + " = h." + k)
                .collect(Collectors.joining(" AND "));
        return "(SELECT h.* FROM " + table + "_hist h WHERE h.hist_made_on <= " + on + " AND NOT EXISTS (SELECT 1 FROM "
                + table + "_hist l WHERE " + same + " AND l.hist_made_on <= " + on + " AND l.hist_seq > h.hist_seq) AND NOT h.hist_gone) "
                + table;
    }
}
