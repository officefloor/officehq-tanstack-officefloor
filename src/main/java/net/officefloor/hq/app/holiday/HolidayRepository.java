package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holidays of each sheet, each date at most once on its sheet. */
@Repository
public class HolidayRepository {

    static final String TAKEN = "That date is already a holiday on this sheet";

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, by date. */
    public List<LocalDate> list(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, n) -> rs.getDate("holiday_date").toLocalDate(), sheetId);
    }

    /** Whether the date is a holiday on the sheet. */
    public boolean taken(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, Date.valueOf(date));
        return n != null && n > 0;
    }

    public void insert(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
    }

    /** The date itself when it is not a holiday on the sheet, else the next day that is not. */
    public LocalDate nextWorkingDay(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query(
                "SELECT holiday_date FROM holiday WHERE sheet_id = ? AND holiday_date >= ?",
                (rs, n) -> rs.getDate("holiday_date").toLocalDate(), sheetId, Date.valueOf(date)));
        LocalDate day = date;
        while (holidays.contains(day)) {
            day = day.plusDays(1);
        }
        return day;
    }
}
