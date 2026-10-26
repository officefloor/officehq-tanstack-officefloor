package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
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

    /**
     * A due-by being fixed now: the {@code terms}-th business day after {@code date}, counting forward from the day after
     * it and skipping every Saturday, Sunday and holiday on the sheet as it now is.
     */
    public LocalDate dueBy(String sheetId, LocalDate date, int terms) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.query("SELECT holiday_date FROM " + AsOf.table("holiday")
                + " WHERE sheet_id = ? AND holiday_date > ?", (rs, n) -> rs.getObject(1, LocalDate.class), sheetId,
                Date.valueOf(date)));
        return businessDays(date, terms, holidays);
    }

    /** The {@code terms}-th day after {@code date} that is neither a Saturday, a Sunday nor one of {@code holidays}. */
    public static LocalDate businessDays(LocalDate date, int terms, Set<LocalDate> holidays) {
        LocalDate due = date;
        for (int left = terms; left > 0;) {
            due = due.plusDays(1);
            if (isBusinessDay(due, holidays)) {
                left--;
            }
        }
        return due;
    }

    public static boolean isBusinessDay(LocalDate day, Set<LocalDate> holidays) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(day);
    }
}
