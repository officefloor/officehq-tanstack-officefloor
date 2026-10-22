package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The holidays by sheet. */
@Repository
public class HolidayRepository {

    private static final RowMapper<Holiday> ROW = (rs, i) -> new Holiday(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getObject("holiday_date", LocalDate.class), rs.getString("added_by"));

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, earliest date first. */
    public List<Holiday> list(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_holiday WHERE sheet_id = ? ORDER BY holiday_date", ROW, sheetId);
    }

    public boolean exists(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_holiday WHERE sheet_id = ? AND holiday_date = ?",
                Integer.class, sheetId, date);
        return n != null && n > 0;
    }

    public Holiday add(String sheetId, HolidayInput input) {
        jdbc.update("INSERT INTO sheet_holiday (sheet_id, holiday_date, added_by) VALUES (?, ?, ?)", sheetId,
                input.date(), input.user());
        Long id = jdbc.queryForObject("SELECT id FROM sheet_holiday WHERE sheet_id = ? AND holiday_date = ?",
                Long.class, sheetId, input.date());
        return new Holiday(id, sheetId, input.date(), input.user());
    }

    /** The date itself when it is not one of the sheet's holidays, else the next day after it that is not one. */
    public LocalDate nextWorkingDay(String sheetId, LocalDate date) {
        Set<LocalDate> on = new HashSet<>(jdbc.queryForList(
                "SELECT holiday_date FROM sheet_holiday WHERE sheet_id = ? AND holiday_date >= ?", LocalDate.class,
                sheetId, date));
        while (on.contains(date)) {
            date = date.plusDays(1);
        }
        return date;
    }
}
