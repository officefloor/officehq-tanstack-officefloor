package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class HolidayRepository {

    private static final RowMapper<Holiday> ROW = (rs, n) -> new Holiday(rs.getString("sheet_id"),
            rs.getObject("holiday", LocalDate.class).toString(), rs.getString("added_by"));

    private final JdbcTemplate jdbc;

    public HolidayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's holidays, by date. */
    public List<Holiday> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, holiday, added_by FROM holiday WHERE sheet_id = ? ORDER BY holiday",
                ROW, sheetId);
    }

    public boolean contains(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE sheet_id = ? AND holiday = ?",
                Integer.class, sheetId, date);
        return n != null && n > 0;
    }

    public Holiday add(String sheetId, LocalDate date, String by) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday, added_by, added_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                sheetId, date, by);
        return new Holiday(sheetId, date.toString(), by);
    }

    /** The due-by of a claim dated {@code date} with {@code terms} (whole business days), by the sheet's
     *  holidays as they are now: see {@link BusinessDays#dueBy}. */
    public LocalDate dueBy(String sheetId, LocalDate date, int terms) {
        Set<LocalDate> holidays = new HashSet<>(jdbc.queryForList(
                "SELECT holiday FROM holiday WHERE sheet_id = ? AND holiday > ?", LocalDate.class, sheetId, date));
        return BusinessDays.dueBy(date, terms, holidays);
    }
}
