package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayStore;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Terms count business days: every claim kept is due by the terms-th business day after its date, skipping
 *  Saturdays, Sundays and the holidays on its sheet now, for the terms it was fixed with. Those terms are
 *  read back from its due-by as it was fixed (its date plus the terms, moved past the sheet's holidays then):
 *  the calendar days from its date to the due-by, less any run of holidays it was moved past. */
public class V43__business_day_due_by extends BaseJavaMigration {

    private record Kept(long id, String sheetId, LocalDate date, LocalDate dueBy) {
    }

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
        List<Kept> claims = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, sheet_id, claim_date, due_by FROM claim");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                claims.add(new Kept(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class),
                        rs.getObject(4, LocalDate.class)));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?")) {
            for (Kept claim : claims) {
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(claim.sheetId(), Set.of());
                LocalDate fixedOn = claim.dueBy();
                while (fixedOn.isAfter(claim.date()) && sheetHolidays.contains(fixedOn.minusDays(1))) {
                    fixedOn = fixedOn.minusDays(1);
                }
                int terms = (int) Math.max(0, ChronoUnit.DAYS.between(claim.date(), fixedOn));
                LocalDate dueBy = HolidayStore.businessDaysAfter(sheetHolidays, claim.date(), terms);
                if (!dueBy.equals(claim.dueBy())) {
                    ps.setDate(1, Date.valueOf(dueBy));
                    ps.setLong(2, claim.id());
                    ps.executeUpdate();
                }
            }
        }
    }
}
