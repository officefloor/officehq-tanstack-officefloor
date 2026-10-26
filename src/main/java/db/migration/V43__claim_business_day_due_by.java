package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.Holidays;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Terms count business days: every claim's due-by, in the store and in its history, is worked out again as the
 * terms-th business day after its date, skipping Saturdays, Sundays and the sheet's holidays as they are now. The
 * terms a claim was fixed with are read back from its due-by: the days from its date to it, less the holidays it was
 * moved past (the run of holidays just before it).
 */
public class V43__claim_business_day_due_by extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT sheet_id, holiday_date FROM holiday");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getObject(2, LocalDate.class));
            }
        }
        // The history first, so the journal's copies of the claims' new due-bys come after it.
        redo(conn, "claim_history", "history_seq", holidays);
        redo(conn, "claim", "id", holidays);
    }

    private static void redo(Connection conn, String table, String key, Map<String, Set<LocalDate>> holidays)
            throws SQLException {
        Map<Object, LocalDate> due = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + key + ", sheet_id, claim_date, due_by FROM " + table);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate date = rs.getObject(3, LocalDate.class);
                LocalDate dueBy = rs.getObject(4, LocalDate.class);
                if (date == null || dueBy == null) {
                    continue;
                }
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(rs.getString(2), Set.of());
                long terms = ChronoUnit.DAYS.between(date, dueBy);
                while (terms > 1 && sheetHolidays.contains(date.plusDays(terms - 1))) {
                    terms--;
                }
                LocalDate redone = Holidays.businessDays(date, (int) Math.max(terms, 0), sheetHolidays);
                if (!redone.equals(dueBy)) {
                    due.put(rs.getObject(1), redone);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE " + table + " SET due_by = ? WHERE " + key + " = ?")) {
            for (Map.Entry<Object, LocalDate> e : due.entrySet()) {
                ps.setDate(1, Date.valueOf(e.getValue()));
                ps.setObject(2, e.getKey());
                ps.executeUpdate();
            }
        }
    }
}
