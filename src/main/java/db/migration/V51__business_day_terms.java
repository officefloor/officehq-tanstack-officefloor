package db.migration;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

/** Terms count business days. Every claim kept is re-fixed to the terms-th business day after its
 *  date, skipping Saturdays, Sundays and the sheet's holidays now. The terms it was fixed with are
 *  the calendar days from its date to its due-by, less the holidays its due-by was moved past. */
public class V51__business_day_terms extends BaseJavaMigration {

    private record Kept(long id, String sheetId, LocalDate date, LocalDate dueBy) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT sheet_id, holiday_date FROM sheet_holiday")) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getDate(2).toLocalDate());
            }
        }
        List<Kept> kept = new ArrayList<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id, sheet_id, claim_date, due_by FROM claim")) {
            while (rs.next()) {
                kept.add(new Kept(rs.getLong(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getDate(4).toLocalDate()));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?")) {
            for (Kept claim : kept) {
                Set<LocalDate> onSheet = holidays.getOrDefault(claim.sheetId(), Set.of());
                LocalDate fixed = claim.dueBy();
                while (fixed.isAfter(claim.date()) && onSheet.contains(fixed.minusDays(1))) fixed = fixed.minusDays(1);
                int terms = (int) Math.max(0, ChronoUnit.DAYS.between(claim.date(), fixed));
                LocalDate dueBy = Holidays.businessDaysAfter(claim.date(), terms, onSheet);
                if (dueBy.equals(claim.dueBy())) continue;
                ps.setDate(1, Date.valueOf(dueBy));
                ps.setLong(2, claim.id());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
