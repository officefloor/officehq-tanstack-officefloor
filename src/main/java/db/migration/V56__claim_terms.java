package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
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
 * Each claim keeps the terms its due-by was fixed with, so adding a holiday can work its due-by out again. For a
 * claim already fixed they are the business days from the date it was fixed from to its due-by, over the sheet's
 * holidays as they are now.
 */
public class V56__claim_terms extends BaseJavaMigration {

    private record Fixed(long id, String sheetId, LocalDate date, LocalDate dueBy) {}

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE claim ADD COLUMN terms INT");
        }
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT sheet_id, holiday_date FROM holiday");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                holidays.computeIfAbsent(rs.getString(1), s -> new HashSet<>()).add(rs.getDate(2).toLocalDate());
            }
        }
        List<Fixed> claims = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT c.id, c.sheet_id, COALESCE((SELECT h.claim_date"
                + " FROM claim_history h WHERE h.claim_id = c.id ORDER BY h.seq LIMIT 1), c.claim_date), c.due_by"
                + " FROM claim c WHERE c.due_by IS NOT NULL");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                claims.add(new Fixed(rs.getLong(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getDate(4).toLocalDate()));
            }
        }
        try (PreparedStatement update = connection.prepareStatement("UPDATE claim SET terms = ? WHERE id = ?")) {
            for (Fixed fixed : claims) {
                update.setInt(1, Holidays.termsBetween(fixed.date(), fixed.dueBy(),
                        holidays.getOrDefault(fixed.sheetId(), Set.of())));
                update.setLong(2, fixed.id());
                update.executeUpdate();
            }
        }
    }
}
