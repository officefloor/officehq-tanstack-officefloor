package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
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

    /** A claim's due-by: the terms-th business day after its date, counting from the day after it and skipping
     *  every Saturday, Sunday and holiday on the sheet as they are now (terms of 0 leave the date itself). */
    public LocalDate dueBy(String sheetId, LocalDate date, int terms) {
        Set<LocalDate> holidays = new HashSet<>();
        jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? AND holiday_date > ?",
                rs -> { holidays.add(rs.getDate(1).toLocalDate()); }, sheetId, Date.valueOf(date));
        return businessDays(date, terms, holidays);
    }

    /** The terms-th day after the date that is neither a Saturday, a Sunday nor one of the holidays. */
    public static LocalDate businessDays(LocalDate date, int terms, Set<LocalDate> holidays) {
        LocalDate due = date;
        for (int counted = 0; counted < terms; ) {
            due = due.plusDays(1);
            DayOfWeek day = due.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !holidays.contains(due)) {
                counted++;
            }
        }
        return due;
    }
}
