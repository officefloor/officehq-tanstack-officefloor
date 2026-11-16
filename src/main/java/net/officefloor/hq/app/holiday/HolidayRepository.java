package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored holidays, kept per sheet. */
@Repository
public class HolidayRepository {

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, by date. */
    public List<LocalDate> list(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, n) -> rs.getObject("holiday_date", LocalDate.class), sheetId);
    }

    /** Adds the holiday; false when the date is already one (the primary key refuses a second). */
    public boolean add(String sheetId, LocalDate date) {
        try {
            jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** The days-th business day after the date: counting forward from the day after it, skipping every Saturday, Sunday
     *  and holiday of the sheet (the date itself when days is 0). */
    public LocalDate businessDaysAfter(String sheetId, LocalDate date, int days) {
        return businessDaysAfter(new HashSet<>(list(sheetId)), date, days);
    }

    /** The days-th business day after the date, skipping every Saturday, Sunday and one of the holidays given. */
    public static LocalDate businessDaysAfter(Set<LocalDate> holidays, LocalDate date, int days) {
        LocalDate day = date;
        for (int counted = 0; counted < days;) {
            day = day.plusDays(1);
            if (isBusinessDay(holidays, day)) {
                counted++;
            }
        }
        return day;
    }

    /** Whether the day is neither a Saturday, a Sunday nor one of the holidays given. */
    public static boolean isBusinessDay(Set<LocalDate> holidays, LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(day);
    }
}
