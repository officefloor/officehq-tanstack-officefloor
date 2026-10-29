package net.officefloor.hq.app.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reading a sheet as it stood at the end of a date: for a statement taken as of a date, and for a screen shown as of
 * one ({@link AsOfFilter}). While {@link #on} runs its work, the stores read the tables a
 * person's figures come from ({@link #table}) out of their history, each row as the last change made while the sheet's
 * today was on or before the date left it (later changes do not count), and that date stands in for the sheet's today.
 */
public final class AsOf {

    /** The columns that name a row of each table kept in history. */
    private static final Map<String, List<String>> KEYS = Map.ofEntries(
            Map.entry("claim", List.of("id")),
            Map.entry("person", List.of("id")),
            Map.entry("person_grade", List.of("person_id", "from_date")),
            Map.entry("person_hold", List.of("id")),
            Map.entry("person_retirement", List.of("id")),
            Map.entry("person_snapshot", List.of("id")),
            Map.entry("rate", List.of("sheet_id", "grade", "from_date")),
            Map.entry("setting", List.of("sheet_id", "setting_key")),
            Map.entry("setting_from", List.of("sheet_id", "setting_key", "from_date")),
            Map.entry("sheet", List.of("id")),
            Map.entry("statement", List.of("id")),
            Map.entry("period", List.of("sheet_id", "name")),
            Map.entry("period_person_total", List.of("sheet_id", "period_name", "person_id")),
            Map.entry("claim_log", List.of("id")),
            Map.entry("claim_comment", List.of("id")),
            Map.entry("claim_line", List.of("id")),
            Map.entry("person_patron", List.of("person_id", "patron_id")),
            Map.entry("person_alias", List.of("id")),
            Map.entry("person_watch", List.of("id")),
            Map.entry("delegation", List.of("id")),
            Map.entry("sheet_role", List.of("sheet_id", "user_name")),
            Map.entry("holiday", List.of("sheet_id", "holiday_date")),
            Map.entry("void_reason", List.of("sheet_id", "reason")),
            Map.entry("settle_batch", List.of("sheet_id", "batch_number")),
            Map.entry("claim_batch", List.of("claim_id")));

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

    /** Reads the store as of {@code date} (as it is now when null) until set back; returns the date it was read as of. */
    static LocalDate set(LocalDate date) {
        LocalDate was = DATE.get();
        DATE.set(date);
        return was;
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
