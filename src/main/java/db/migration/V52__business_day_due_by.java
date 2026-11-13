package db.migration;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.DatedSettings;
import net.officefloor.hq.app.setting.SettingRepository;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Terms count business days: every claim's due-by becomes the terms-th business day after its date, skipping Saturdays,
 * Sundays and the sheet's holidays as they stand now. The terms a claim was fixed with are those of its person in force
 * on its date when they give its stored due-by (calendar days, moved past holidays); otherwise the calendar days from
 * its date to its stored due-by.
 */
public class V52__business_day_due_by extends BaseJavaMigration {

    private record Row(long id, String sheetId, long personId, LocalDate date, LocalDate dueBy) {
    }

    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        List<Row> rows = jdbc.query("SELECT id, sheet_id, person_id, claim_date, due_by FROM claim",
                (rs, n) -> new Row(rs.getLong("id"), rs.getString("sheet_id"), rs.getLong("person_id"),
                        rs.getDate("claim_date").toLocalDate(), rs.getDate("due_by").toLocalDate()));
        if (rows.isEmpty()) {
            return;
        }
        HolidayRepository holidayRepository = new HolidayRepository(jdbc);
        PersonRepository people = new PersonRepository(jdbc);
        SettingRepository settings = new SettingRepository(jdbc);
        Map<String, Set<LocalDate>> holidays = new HashMap<>();
        Map<String, DatedSettings> dated = new HashMap<>();
        for (Row row : rows) {
            Set<LocalDate> sheetHolidays = holidays.computeIfAbsent(row.sheetId(), holidayRepository::dates);
            Person person = people.find(row.sheetId(), row.personId());
            int terms = (int) ChronoUnit.DAYS.between(row.date(), row.dueBy());
            if (person != null) {
                int inForce = person.termsOn(dated.computeIfAbsent(row.sheetId(), settings::dated), row.date());
                if (calendarDueBy(sheetHolidays, row.date(), inForce).equals(row.dueBy())) {
                    terms = inForce;
                }
            }
            LocalDate dueBy = HolidayRepository.businessDaysAfter(sheetHolidays, row.date(), Math.max(terms, 0));
            jdbc.update("UPDATE claim SET due_by = ? WHERE id = ?", Date.valueOf(dueBy), row.id());
        }
    }

    /** The due-by as it was fixed before: the date plus the terms, moved past any holiday to the next day not one. */
    private static LocalDate calendarDueBy(Set<LocalDate> holidays, LocalDate date, int terms) {
        LocalDate day = date.plusDays(terms);
        while (holidays.contains(day)) {
            day = day.plusDays(1);
        }
        return day;
    }
}
