package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays store, kept per sheet. */
@Repository
public class HolidayRepository {

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, by date. */
    public List<Holiday> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, holiday_date FROM sheet_holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, n) -> new Holiday(rs.getString("sheet_id"), rs.getDate("holiday_date").toLocalDate()), sheetId);
    }

    public boolean exists(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, Date.valueOf(date));
        return n != null && n > 0;
    }

    public void save(Holiday holiday) {
        jdbc.update("INSERT INTO sheet_holiday (sheet_id, holiday_date) VALUES (?, ?)", holiday.sheetId(),
                Date.valueOf(holiday.date()));
    }

    /**
     * The due-by of a claim dated {@code date} with {@code terms}: the terms-th business day after its
     * date, counting forward from the day after it and skipping every Saturday, Sunday and holiday on
     * the sheet now. With terms 0 (or less) it is the date itself.
     */
    public LocalDate dueBy(String sheetId, LocalDate date, int terms) {
        Set<LocalDate> holidays = new HashSet<>();
        for (Holiday h : findAll(sheetId)) holidays.add(h.date());
        return businessDaysAfter(date, terms, holidays);
    }

    /** The {@code days}-th day after {@code date} that is neither a Saturday, a Sunday nor one of {@code holidays}. */
    public static LocalDate businessDaysAfter(LocalDate date, int days, Set<LocalDate> holidays) {
        LocalDate day = date;
        for (int left = days; left > 0; ) {
            day = day.plusDays(1);
            DayOfWeek dow = day.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(day)) left--;
        }
        return day;
    }
}
