package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays on each sheet. */
@Repository
public class Holidays {

    private final JdbcTemplate jdbc;

    public Holidays(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every holiday on the sheet (yyyy-MM-dd), earliest first. */
    public List<String> list(String sheet) {
        return jdbc.query("SELECT holiday_on FROM holiday WHERE sheet = ? ORDER BY holiday_on",
                (rs, i) -> rs.getDate("holiday_on").toLocalDate().toString(), sheet);
    }

    /** Whether the date is already on the sheet's list. */
    public boolean has(String sheet, LocalDate date) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet = ? AND holiday_on = ?", Integer.class,
                sheet, Date.valueOf(date));
        return found != null && found > 0;
    }

    public void add(String sheet, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet, holiday_on) VALUES (?, ?)", sheet, Date.valueOf(date));
    }

    /**
     * The days-th business day after the date: counting forward from the day after it, skipping every Saturday, Sunday
     * and holiday on the sheet (as they stand now). No days is the date itself.
     */
    public LocalDate businessDaysAfter(String sheet, LocalDate date, int days) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query("SELECT holiday_on FROM holiday WHERE sheet = ? AND holiday_on > ?",
                (rs, i) -> rs.getDate("holiday_on").toLocalDate(), sheet, Date.valueOf(date)));
        return businessDaysAfter(date, days, holidays);
    }

    /** The days-th day after the date that is neither a Saturday, a Sunday nor one of the holidays. */
    public static LocalDate businessDaysAfter(LocalDate date, int days, Set<LocalDate> holidays) {
        LocalDate on = date;
        for (int left = days; left > 0;) {
            on = on.plusDays(1);
            DayOfWeek day = on.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !holidays.contains(on)) {
                left--;
            }
        }
        return on;
    }
}
