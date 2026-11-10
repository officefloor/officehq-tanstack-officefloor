package db.migration;

import java.sql.Connection;
import java.sql.Date;
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
 * A claim keeps the terms its due-by was fixed with, so adding a holiday can fix it again. The terms of
 * a claim already stored are recovered from its due-by: the fewest business days after its date (skipping
 * Saturdays, Sundays and the holidays on its sheet now) that reach it.
 */
public class V59__claim_terms extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection db = context.getConnection();
        try (Statement s = db.createStatement()) {
            s.execute("ALTER TABLE claim ADD COLUMN terms INT DEFAULT 0 NOT NULL");
        }
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement q = db.prepareStatement("SELECT sheet_id, holiday_date FROM sheet_holiday");
                ResultSet rs = q.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getDate(2).toLocalDate());
            }
        }
        try (PreparedStatement q = db.prepareStatement("SELECT id, sheet_id, claim_date, due_by FROM claim");
                PreparedStatement u = db.prepareStatement("UPDATE claim SET terms = ? WHERE id = ?");
                ResultSet rs = q.executeQuery()) {
            while (rs.next()) {
                Set<LocalDate> onSheet = holidays.getOrDefault(rs.getString(2), Set.of());
                LocalDate date = rs.getDate(3).toLocalDate();
                LocalDate dueBy = rs.getDate(4).toLocalDate();
                int terms = 0;
                while (HolidayRepository.businessDaysAfter(date, terms, onSheet).isBefore(dueBy)) terms++;
                if (terms > 0) {
                    u.setInt(1, terms);
                    u.setLong(2, rs.getLong(1));
                    u.addBatch();
                }
            }
            u.executeBatch();
        }
    }
}
