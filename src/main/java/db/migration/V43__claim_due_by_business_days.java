package db.migration;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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
 * Terms count business days: every claim already stored has its due-by worked out again as the terms-th business day after
 * its date (skipping Saturdays, Sundays and the sheet's holidays as they are now). The terms a claim was fixed with are its
 * due-by less its date, less the run of holidays just before its due-by that it was moved past as it was added.
 */
public class V43__claim_due_by_business_days extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (Statement st = context.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT sheet_id, holiday_date FROM holiday")) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), (s) -> new HashSet<>()).add(rs.getObject(2, LocalDate.class));
            }
        }
        try (Statement st = context.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT id, sheet_id, claim_date, due_by FROM claim");
                PreparedStatement update = context.getConnection().prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?")) {
            while (rs.next()) {
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(rs.getString(2), Set.of());
                LocalDate date = rs.getObject(3, LocalDate.class);
                LocalDate dueBy = rs.getObject(4, LocalDate.class);
                LocalDate target = dueBy;
                while (target.isAfter(date) && sheetHolidays.contains(target.minusDays(1))) {
                    target = target.minusDays(1);
                }
                int terms = (int) Math.max(0, ChronoUnit.DAYS.between(date, target));
                update.setDate(1, Date.valueOf(HolidayRepository.businessDaysAfter(sheetHolidays, date, terms)));
                update.setLong(2, rs.getLong(1));
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
