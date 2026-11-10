package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayRepository;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Terms now count business days. Every due-by already stored (on a claim and in its history) is worked
 * out again as the terms-th business day after its claim's date, skipping Saturdays, Sundays and the
 * holidays on its sheet now, with the terms it was fixed with. Those terms are recovered from the
 * calendar due-by kept: its days after the claim's date, less the holidays it was moved past (the run
 * of holidays on the sheet immediately before it).
 */
public class V52__business_day_terms extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection db = context.getConnection();
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement q = db.prepareStatement("SELECT sheet_id, holiday_date FROM sheet_holiday");
                ResultSet rs = q.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getDate(2).toLocalDate());
            }
        }
        rework(db, "claim", holidays);
        rework(db, "claim_history", holidays);
    }

    private static void rework(Connection db, String table, Map<String, Set<LocalDate>> holidays) throws Exception {
        try (PreparedStatement q = db.prepareStatement("SELECT id, sheet_id, claim_date, due_by FROM " + table);
                PreparedStatement u = db.prepareStatement("UPDATE " + table + " SET due_by = ? WHERE id = ?");
                ResultSet rs = q.executeQuery()) {
            while (rs.next()) {
                Set<LocalDate> onSheet = holidays.getOrDefault(rs.getString(2), Set.of());
                LocalDate date = rs.getDate(3).toLocalDate();
                LocalDate dueBy = rs.getDate(4).toLocalDate();
                LocalDate target = dueBy;
                while (target.isAfter(date) && onSheet.contains(target.minusDays(1))) target = target.minusDays(1);
                int terms = (int) ChronoUnit.DAYS.between(date, target);
                LocalDate reworked = HolidayRepository.businessDaysAfter(date, terms, onSheet);
                if (!reworked.equals(dueBy)) {
                    u.setDate(1, Date.valueOf(reworked));
                    u.setLong(2, rs.getLong(1));
                    u.addBatch();
                }
            }
            u.executeBatch();
        }
    }
}
