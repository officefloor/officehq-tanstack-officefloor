package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.Holidays;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * The terms a claim's due-by was fixed with, kept with it (in its history too, see V39) so that a holiday added later
 * can work the due-by out again from its date. For a claim made before, they are read back from its due-by: the business
 * days from the day after its date up to it, over the sheet's holidays as they are now. The journal trigger is made
 * again around the change, as in V41.
 */
public class V49__claim_due_terms extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TRIGGER claim_journal");
            st.execute("ALTER TABLE claim ADD COLUMN due_terms INT");
            st.execute("ALTER TABLE claim_history ADD COLUMN due_terms INT");
        }
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT sheet_id, holiday_date FROM holiday");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getObject(2, LocalDate.class));
            }
        }
        backfill(conn, "claim_history", "history_seq", holidays);
        backfill(conn, "claim", "id", holidays);
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TRIGGER claim_journal AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW CALL "
                    + "'net.officefloor.hq.app.history.Journal'");
        }
    }

    private static void backfill(Connection conn, String table, String key, Map<String, Set<LocalDate>> holidays)
            throws SQLException {
        Map<Object, Integer> terms = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + key + ", sheet_id, claim_date, due_by FROM " + table);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalDate date = rs.getObject(3, LocalDate.class);
                LocalDate dueBy = rs.getObject(4, LocalDate.class);
                if (date == null || dueBy == null) {
                    continue;
                }
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(rs.getString(2), Set.of());
                int n = 0;
                for (LocalDate day = date.plusDays(1); !day.isAfter(dueBy); day = day.plusDays(1)) {
                    if (Holidays.isBusinessDay(day, sheetHolidays)) {
                        n++;
                    }
                }
                terms.put(rs.getObject(1), n);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE " + table + " SET due_terms = ? WHERE " + key + " = ?")) {
            for (Map.Entry<Object, Integer> e : terms.entrySet()) {
                ps.setInt(1, e.getValue());
                ps.setObject(2, e.getKey());
                ps.executeUpdate();
            }
        }
    }
}
