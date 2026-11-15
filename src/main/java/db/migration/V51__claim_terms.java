package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
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
 * A claim keeps the terms its due-by was fixed with, so a holiday added later can work its due-by out
 * again. Those of an existing claim are recovered from its due-by: the business days from the date it was
 * fixed from (its first recorded date, as a re-date or an edit leaves the due-by as it was) up to its
 * due-by, past the holidays that were on the sheet when the claim was added.
 */
public class V51__claim_terms extends BaseJavaMigration {

    private record Fixed(long id, String sheetId, LocalDate date, LocalDate dueBy, Timestamp createdAt) {
    }

    private record Holiday(LocalDate day, Timestamp addedAt) {
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE claim ADD COLUMN terms INT");
        }
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
        try (PreparedStatement update = connection.prepareStatement("UPDATE claim SET terms = ? WHERE id = ?")) {
            for (Fixed c : claims) {
                Set<LocalDate> then = new HashSet<>();
                for (Holiday h : holidays.getOrDefault(c.sheetId(), List.of())) {
                    if (!h.addedAt().after(c.createdAt())) then.add(h.day());
                }
                update.setInt(1, BusinessDays.between(c.date(), c.dueBy(), then));
                update.setLong(2, c.id());
                update.addBatch();
            }
            update.executeBatch();
        }
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE claim ALTER COLUMN terms SET NOT NULL");
        }
    }
}
