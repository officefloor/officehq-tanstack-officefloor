package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.history.AsOf;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the sheet's holidays, each date at most once per sheet. */
@Repository
public class Holidays {

    private final JdbcTemplate jdbc;

    public Holidays(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, earliest first. */
    public List<LocalDate> list(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM " + AsOf.table("holiday") + " WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, n) -> rs.getObject(1, LocalDate.class), sheetId);
    }

    public boolean has(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + AsOf.table("holiday") + " WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, Date.valueOf(date));
        return n != null && n > 0;
    }

    public void add(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
    }

    /** A due-by being fixed now: {@code date} itself, else, when it is a holiday, the next day that is not one. */
    public LocalDate dueBy(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query("SELECT holiday_date FROM " + AsOf.table("holiday")
                + " WHERE sheet_id = ? AND holiday_date >= ?", (rs, n) -> rs.getObject(1, LocalDate.class), sheetId,
                Date.valueOf(date)));
        LocalDate due = date;
        while (holidays.contains(due)) {
            due = due.plusDays(1);
        }
        return due;
    }
}
