package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HolidayRepository {

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, earliest first. */
    public List<Holiday> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, n) -> new Holiday(rs.getString("sheet_id"), rs.getDate("holiday_date").toLocalDate()), sheetId);
    }

    public boolean exists(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, Date.valueOf(date));
        return n != null && n > 0;
    }

    public void add(Holiday holiday) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", holiday.sheetId(),
                Date.valueOf(holiday.date()));
    }

    /** The sheet's holidays as they stand now. */
    public Set<LocalDate> dates(String sheetId) {
        Set<LocalDate> holidays = new HashSet<>();
        for (Holiday h : findAll(sheetId)) {
            holidays.add(h.date());
        }
        return holidays;
    }

    /** The {@code days}-th business day after the date on the sheet: counting from the day after it, skipping every
     * Saturday, Sunday and one of the sheet's holidays (as the list stands now); the date itself when {@code days} is 0. */
    public LocalDate businessDaysAfter(String sheetId, LocalDate date, int days) {
        return businessDaysAfter(dates(sheetId), date, days);
    }

    /** The {@code days}-th business day after the date, skipping every Saturday, Sunday and one of the holidays. */
    public static LocalDate businessDaysAfter(Set<LocalDate> holidays, LocalDate date, int days) {
        LocalDate day = date;
        int left = days;
        while (left > 0) {
            day = day.plusDays(1);
            if (isBusinessDay(holidays, day)) {
                left--;
            }
        }
        return day;
    }

    /** How many business days there are after {@code from} up to and including {@code to}: the terms that fix
     * {@code to} as the due-by of a claim dated {@code from} (0 when {@code to} is not after {@code from}). */
    public static int businessDaysBetween(Set<LocalDate> holidays, LocalDate from, LocalDate to) {
        int days = 0;
        for (LocalDate day = from.plusDays(1); !day.isAfter(to); day = day.plusDays(1)) {
            if (isBusinessDay(holidays, day)) {
                days++;
            }
        }
        return days;
    }

    private static boolean isBusinessDay(Set<LocalDate> holidays, LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(day);
    }
}
