package db.migration;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.BusinessDays;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Works out every existing claim's due-by in business days: the terms-th business day after its due-from. */
public class V50__claim_business_day_due_by extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        try (Statement select = context.getConnection().createStatement();
                ResultSet rows = select.executeQuery("SELECT sheet_id, holiday_date FROM holiday")) {
            while (rows.next()) {
                holidays.computeIfAbsent(rows.getString(1), k -> new HashSet<>()).add(rows.getDate(2).toLocalDate());
            }
        }
        try (Statement select = context.getConnection().createStatement();
                ResultSet rows = select.executeQuery("SELECT id, sheet_id, due_from, due_terms FROM claim");
                PreparedStatement update = context.getConnection().prepareStatement("UPDATE claim SET due_by = ? WHERE id = ?")) {
            while (rows.next()) {
                LocalDate dueBy = BusinessDays.after(rows.getDate(3).toLocalDate(), rows.getInt(4),
                        holidays.getOrDefault(rows.getString(2), Set.of()));
                update.setDate(1, Date.valueOf(dueBy));
                update.setLong(2, rows.getLong(1));
                update.executeUpdate();
            }
        }
    }
}
