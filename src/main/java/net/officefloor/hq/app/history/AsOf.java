package net.officefloor.hq.app.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reading a sheet as it stood at the end of a date. While {@link #on} runs its work, the stores read the tables a
 * person's figures come from ({@link #table}) out of their history, each row as the last change made while the sheet's
 * today was on or before the date left it (later changes do not count), and that date stands in for the sheet's today.
 */
public final class AsOf {

    /** The columns that name a row of each table kept in history. */
    private static final Map<String, List<String>> KEYS = Map.of(
            "claim", List.of("id"),
            "person", List.of("id"),
            "person_grade", List.of("person_id", "from_date"),
            "person_hold", List.of("id"),
            "rate", List.of("sheet_id", "grade", "from_date"),
            "setting", List.of("sheet_id", "setting_key"),
            "setting_from", List.of("sheet_id", "setting_key", "from_date"));

    private static final ThreadLocal<LocalDate> DATE = new ThreadLocal<>();

    private AsOf() {
    }

    /** Runs the work reading the store as it stood at the end of {@code date}. */
    public static <T> T on(LocalDate date, Supplier<T> work) {
        LocalDate was = DATE.get();
        DATE.set(date);
        try {
            return work.get();
        } finally {
            DATE.set(was);
        }
    }

    /** The date the store is read as of; null when it is read as it is now. */
    public static LocalDate date() {
        return DATE.get();
    }

    /** The table to read FROM, named {@code table} in the query: as it is now, or as it stood at the end of the date. */
    public static String table(String table) {
        return table(table, table);
    }

    /** The table to read FROM, named {@code alias} in the query: as it is now, or as it stood at the end of the date. */
    public static String table(String table, String alias) {
        LocalDate date = DATE.get();
        if (date == null) {
            return table + " " + alias;
        }
        String on = "DATE '" + date + "'";
        StringBuilder same = new StringBuilder();
        for (String key : KEYS.get(table)) {
            same.append(" AND x.").append(key).append(" IS NOT DISTINCT FROM h.").append(key);
        }
        return "(SELECT * FROM " + table + "_history h WHERE h.history_on <= " + on + " AND NOT h.history_gone"
                + " AND NOT EXISTS (SELECT 1 FROM " + table + "_history x WHERE x.history_on <= " + on
                + " AND x.history_seq > h.history_seq" + same + ")) " + alias;
    }
}
