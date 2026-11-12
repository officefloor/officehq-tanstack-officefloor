package net.officefloor.hq.app.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays per sheet. A claim's due-by, fixed when it is added, counts business days past those on the
 *  sheet then (see {@link #businessDaysAfter}); a holiday added later leaves existing due-by dates as they are. */
@Repository
public class HolidayStore {

    private final JdbcTemplate jdbc;

    public HolidayStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, earliest first. */
    public List<LocalDate> list(String sheetId) {
        return jdbc.queryForList("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                LocalDate.class, sheetId);
    }

    /** The sheet's holidays at the end of the date (see {@link History}), earliest first. */
    public List<LocalDate> listAsOf(String sheetId, LocalDate date) {
        return jdbc.queryForList("SELECT holiday_date FROM " + History.asOf("holiday", "id", date)
                + " WHERE sheet_id = ? ORDER BY holiday_date", LocalDate.class, sheetId);
    }

    public boolean has(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, date);
        return n != null && n > 0;
    }

    public void add(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, date);
    }

    /** The {@code days}-th business day after the date: counting forward from the day after it, skipping every
     *  Saturday, Sunday and holiday on the sheet now. The date itself when {@code days} is 0. */
    public LocalDate businessDaysAfter(String sheetId, LocalDate date, int days) {
        return businessDaysAfter(new HashSet<>(list(sheetId)), date, days);
    }

    /** The {@code days}-th business day after the date, skipping weekends and the given holidays. */
    public static LocalDate businessDaysAfter(Set<LocalDate> holidays, LocalDate date, int days) {
        LocalDate d = date;
        for (int n = 0; n < days; ) {
            d = d.plusDays(1);
            if (isBusinessDay(holidays, d)) {
                n++;
            }
        }
        return d;
    }

    private static boolean isBusinessDay(Set<LocalDate> holidays, LocalDate d) {
        DayOfWeek day = d.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !holidays.contains(d);
    }
}
