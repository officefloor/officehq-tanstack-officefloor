package db.migration;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayRepository;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * The terms (business days) a claim's due-by was fixed with, kept so the due-by can be worked out again when a holiday
 * is added. An existing claim takes the business days from its date to its stored due-by, over the sheet's holidays as
 * they stand now.
 */
public class V58__claim_fixed_terms extends BaseJavaMigration {

    private record Row(long id, String sheetId, LocalDate date, LocalDate dueBy) {
    }

    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        jdbc.execute("ALTER TABLE claim ADD COLUMN fixed_terms INT NOT NULL DEFAULT 0");
        List<Row> rows = jdbc.query("SELECT id, sheet_id, claim_date, due_by FROM claim",
                (rs, n) -> new Row(rs.getLong("id"), rs.getString("sheet_id"),
                        rs.getDate("claim_date").toLocalDate(), rs.getDate("due_by").toLocalDate()));
        HolidayRepository holidayRepository = new HolidayRepository(jdbc);
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        for (Row row : rows) {
            Set<LocalDate> sheetHolidays = holidays.computeIfAbsent(row.sheetId(), holidayRepository::dates);
            jdbc.update("UPDATE claim SET fixed_terms = ? WHERE id = ?",
                    HolidayRepository.businessDaysBetween(sheetHolidays, row.date(), row.dueBy()), row.id());
        }
    }
}
