package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays of each sheet: a date is on a sheet's list at most once. */
@Repository
public class Holidays {

    private final JdbcTemplate jdbc;

    public Holidays(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every holiday on the sheet, earliest first. */
    public List<LocalDate> onSheet(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM sheet_holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
    }

    /** Whether `date` is on the sheet's list. */
    public boolean has(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, Date.valueOf(date));
        return n != null && n > 0;
    }

    /** The `days`-th business day after `date`: counting forward from the day after it, skipping
     *  every Saturday, Sunday and holiday on the sheet. `date` itself when `days` is not above 0. */
    public LocalDate businessDaysAfter(String sheetId, LocalDate date, int days) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query(
                "SELECT holiday_date FROM sheet_holiday WHERE sheet_id = ? AND holiday_date > ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId, Date.valueOf(date)));
        return businessDaysAfter(date, days, holidays);
    }

    /** The `days`-th day after `date` that is neither a Saturday, a Sunday nor one of `holidays`. */
    public static LocalDate businessDaysAfter(LocalDate date, int days, Set<LocalDate> holidays) {
        LocalDate day = date;
        for (int left = days; left > 0; ) {
            day = day.plusDays(1);
            if (isBusinessDay(day, holidays)) left--;
        }
        return day;
    }

    /** How many days after `date`, up to and including `until`, are neither a Saturday, a Sunday nor
     *  one of `holidays`: the terms `businessDaysAfter` turns back into `until`. */
    public static int businessDaysBetween(LocalDate date, LocalDate until, Set<LocalDate> holidays) {
        int days = 0;
        for (LocalDate day = date.plusDays(1); !day.isAfter(until); day = day.plusDays(1)) {
            if (isBusinessDay(day, holidays)) days++;
        }
        return days;
    }

    private static boolean isBusinessDay(LocalDate day, Set<LocalDate> holidays) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(day);
    }

    public void add(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO sheet_holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
    }
}
