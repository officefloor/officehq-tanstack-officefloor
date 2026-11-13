package net.officefloor.hq.app.holiday;

import java.sql.Date;
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

    /** The date, or when it is one of the sheet's holidays, the next day after it that is not. */
    public LocalDate nextWorkingDay(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>();
        for (Holiday h : findAll(sheetId)) {
            holidays.add(h.date());
        }
        LocalDate day = date;
        while (holidays.contains(day)) {
            day = day.plusDays(1);
        }
        return day;
    }
}
