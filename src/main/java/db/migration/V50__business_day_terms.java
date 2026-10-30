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
import net.officefloor.hq.app.holiday.Holidays;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Terms now count business days: every claim's due-by is worked out again as the terms-th business day after the
 * date it was fixed from, skipping Saturdays, Sundays and the sheet's holidays as they are now. The terms it was
 * fixed with are its old due-by less that date, less the run of holidays its due-by was moved past.
 */
public class V50__business_day_terms extends BaseJavaMigration {

    private record Fixed(long id, String sheetId, LocalDate date, LocalDate dueBy) {}

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT sheet_id, holiday_date FROM holiday");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), s -> new HashSet<>()).add(rs.getDate(2).toLocalDate());
            }
        }
        // The date a due-by was fixed from is the claim's first recorded date: a redate leaves the due-by as it is.
        List<Fixed> claims = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT c.id, c.sheet_id, COALESCE((SELECT h.claim_date"
                + " FROM claim_history h WHERE h.claim_id = c.id ORDER BY h.seq LIMIT 1), c.claim_date), c.due_by"
                + " FROM claim c WHERE c.due_by IS NOT NULL");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                claims.add(new Fixed(rs.getLong(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getDate(4).toLocalDate()));
            }
        }
        try (PreparedStatement claim = connection.prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?");
                PreparedStatement history = connection.prepareStatement(
                        "UPDATE claim_history SET due_by = ? WHERE claim_id = ? AND due_by IS NOT NULL")) {
            for (Fixed fixed : claims) {
                Set<LocalDate> sheetHolidays = holidays.getOrDefault(fixed.sheetId(), Set.of());
                LocalDate moved = fixed.dueBy();
                while (moved.isAfter(fixed.date()) && sheetHolidays.contains(moved.minusDays(1))) {
                    moved = moved.minusDays(1);
                }
                int terms = (int) Math.max(0, ChronoUnit.DAYS.between(fixed.date(), moved));
                Date dueBy = Date.valueOf(Holidays.businessDays(fixed.date(), terms, sheetHolidays));
                claim.setDate(1, dueBy);
                claim.setLong(2, fixed.id());
                claim.executeUpdate();
                history.setDate(1, dueBy);
                history.setLong(2, fixed.id());
                history.executeUpdate();
            }
        }
    }
}
