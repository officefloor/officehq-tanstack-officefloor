package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayRepository;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A claim keeps the terms its due-by was fixed with, so a holiday added later can work its due-by out again. Every claim
 * already stored is given the business days from its date up to its due-by (skipping Saturdays, Sundays and the sheet's
 * holidays as they are now).
 */
public class V50__claim_terms extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement st = context.getConnection().createStatement()) {
            st.execute("ALTER TABLE claim ADD COLUMN terms INT");
        }
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (Statement st = context.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT sheet_id, holiday_date FROM holiday")) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), (s) -> new HashSet<>()).add(rs.getObject(2, LocalDate.class));
            }
        }
        try (Statement st = context.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT id, sheet_id, claim_date, due_by FROM claim");
                PreparedStatement update = context.getConnection().prepareStatement("UPDATE claim SET terms = ? WHERE id = ?")) {
            while (rs.next()) {
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(rs.getString(2), Set.of());
                LocalDate date = rs.getObject(3, LocalDate.class);
                LocalDate dueBy = rs.getObject(4, LocalDate.class);
                int terms = 0;
                for (LocalDate day = date.plusDays(1); !day.isAfter(dueBy); day = day.plusDays(1)) {
                    if (HolidayRepository.isBusinessDay(sheetHolidays, day)) {
                        terms++;
                    }
                }
                update.setInt(1, terms);
                update.setLong(2, rs.getLong(1));
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
