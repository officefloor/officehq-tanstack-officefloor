package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Keeps the terms each claim's due-by was fixed with, so that adding a holiday can work its due-by out again
 *  (see {@code HolidayStore}). Every claim kept takes the business days from its date to its due-by, over the
 *  sheet's holidays now (its due-by counts them since V43). Kept in claim_hist too; the history trigger is
 *  set again afterwards, as in V45__claim_tags.sql. */
public class V49__claim_due_terms extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TRIGGER claim_history");
            st.execute("ALTER TABLE claim ADD COLUMN due_terms INT");
            st.execute("ALTER TABLE claim_hist ADD COLUMN due_terms INT");
        }
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT sheet_id, holiday_date FROM holiday");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getObject(2, LocalDate.class));
            }
        }
        try (PreparedStatement select = conn.prepareStatement("SELECT id, sheet_id, claim_date, due_by FROM claim");
                PreparedStatement update = conn.prepareStatement("UPDATE claim SET due_terms = ? WHERE id = ?");
                ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(rs.getString(2), Set.of());
                LocalDate date = rs.getObject(3, LocalDate.class);
                LocalDate dueBy = rs.getObject(4, LocalDate.class);
                int terms = 0;
                for (LocalDate d = date.plusDays(1); !d.isAfter(dueBy); d = d.plusDays(1)) {
                    if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY
                            && !sheetHolidays.contains(d)) {
                        terms++;
                    }
                }
                update.setInt(1, terms);
                update.setLong(2, rs.getLong(1));
                update.executeUpdate();
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TRIGGER claim_history AFTER INSERT, UPDATE, DELETE ON claim FOR EACH ROW "
                    + "CALL 'net.officefloor.hq.app.history.HistoryTrigger'");
        }
    }
}
