package net.officefloor.hq.app.holiday;

import java.sql.Date;
import java.time.DayOfWeek;
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

    /** The sheet's holidays, as they stand now. */
    public Set<LocalDate> set(String sheetId) {
        return new HashSet<>(list(sheetId));
    }

    /** The due-by of a claim counting from {@code from} with {@code terms}, by the sheet's holidays as they stand now. */
    public LocalDate dueBy(String sheetId, LocalDate from, int terms) {
        return businessDay(from, terms, set(sheetId));
    }

    /**
     * The {@code terms}-th business day after {@code from}: counting forward from the day after it, skipping every
     * Saturday, Sunday and holiday. The date itself for no terms.
     */
    public static LocalDate businessDay(LocalDate from, int terms, Set<LocalDate> holidays) {
        LocalDate day = from;
        for (int counted = 0; counted < terms;) {
            day = day.plusDays(1);
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !holidays.contains(day)) {
                counted++;
            }
        }
        return day;
    }
}
