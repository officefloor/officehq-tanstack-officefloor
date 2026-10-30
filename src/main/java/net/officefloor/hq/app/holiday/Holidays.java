package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The holiday store: the dates on each sheet's list of holidays. */
@Repository
public class Holidays {

    private final JdbcTemplate jdbc;

    public Holidays(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays (YYYY-MM-DD), earliest first. */
    public List<String> list(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, i) -> rs.getDate(1).toLocalDate().toString(), sheetId);
    }

    /** Whether the date is on the sheet's list. */
    public boolean contains(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Adds the date to the sheet's list. */
    public void add(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
    }

    /** A due-by moved past the sheet's holidays as they are now: the date when it is not a holiday, else the next day that is not. */
    public LocalDate dueBy(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>();
        jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? AND holiday_date >= ?",
                rs -> { holidays.add(rs.getDate(1).toLocalDate()); }, sheetId, Date.valueOf(date));
        LocalDate due = date;
        while (holidays.contains(due)) {
            due = due.plusDays(1);
        }
        return due;
    }
}
