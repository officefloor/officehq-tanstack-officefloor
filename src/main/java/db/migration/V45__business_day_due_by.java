package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.BusinessDays;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Terms count business days. Every claim's due-by is worked out again as the terms-th business day after
 * the date it was fixed from, skipping weekends and the sheet's holidays as they are now. The terms a
 * claim was fixed with are recovered from its due-by: the days from the date it was fixed from (its
 * first recorded date, as a re-date leaves the due-by as it was) to its due-by, less the holidays it was
 * moved past when it was fixed (those already on the sheet when the claim was added). Its recorded
 * history takes the same due-by.
 */
public class V45__business_day_due_by extends BaseJavaMigration {

    private record Fixed(long id, String sheetId, LocalDate date, LocalDate dueBy, Timestamp createdAt) {
    }

    private record Holiday(LocalDate day, Timestamp addedAt) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<String, List<Holiday>> holidays = new HashMap<>();
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery("SELECT sheet_id, holiday, added_at FROM holiday")) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString("sheet_id"), (k) -> new ArrayList<>())
                        .add(new Holiday(rs.getObject("holiday", LocalDate.class), rs.getTimestamp("added_at")));
            }
        }
        List<Fixed> claims = new ArrayList<>();
        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery("SELECT c.id, c.sheet_id, c.due_by, c.created_at, COALESCE("
                        + "(SELECT h.claim_date FROM claim_history h WHERE h.claim_id = c.id ORDER BY h.id LIMIT 1), "
                        + "c.claim_date) AS fixed_date FROM claim c")) {
            while (rs.next()) {
                claims.add(new Fixed(rs.getLong("id"), rs.getString("sheet_id"),
                        rs.getObject("fixed_date", LocalDate.class), rs.getObject("due_by", LocalDate.class),
                        rs.getTimestamp("created_at")));
            }
        }
        try (PreparedStatement claim = connection.prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?");
                PreparedStatement history = connection.prepareStatement(
                        "UPDATE claim_history SET due_by = ? WHERE claim_id = ?")) {
            for (Fixed c : claims) {
                List<Holiday> onSheet = holidays.getOrDefault(c.sheetId(), List.of());
                Set<LocalDate> then = new HashSet<>();
                Set<LocalDate> now = new HashSet<>();
                for (Holiday h : onSheet) {
                    now.add(h.day());
                    if (!h.addedAt().after(c.createdAt())) then.add(h.day());
                }
                // Undo the move past the holidays then: the due-by before it was the first of the run of
                // holidays just before it.
                LocalDate unmoved = c.dueBy();
                while (unmoved.isAfter(c.date()) && then.contains(unmoved.minusDays(1))) unmoved = unmoved.minusDays(1);
                int terms = (int) Math.max(0, ChronoUnit.DAYS.between(c.date(), unmoved));
                LocalDate dueBy = BusinessDays.dueBy(c.date(), terms, now);
                claim.setObject(1, dueBy);
                claim.setLong(2, c.id());
                claim.addBatch();
                history.setObject(1, dueBy);
                history.setLong(2, c.id());
                history.addBatch();
            }
            claim.executeBatch();
            history.executeBatch();
        }
    }
}
