package net.officefloor.hq.app.holiday;

import java.sql.Date;
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

    /** `date`, or when it is a holiday on the sheet, the next day that is not one. */
    public LocalDate nextWorking(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query(
                "SELECT holiday_date FROM sheet_holiday WHERE sheet_id = ? AND holiday_date >= ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId, Date.valueOf(date)));
        LocalDate day = date;
        while (holidays.contains(day)) day = day.plusDays(1);
        return day;
    }

    public void add(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO sheet_holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
    }
}
