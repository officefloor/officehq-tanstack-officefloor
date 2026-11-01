package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * The sheet's "today". Every date decision and every default date in the app reads the date from
 * here, never from the real clock. A sheet starts at the real date until its today is set.
 */
@Service
public class SheetClock {

    private final JdbcTemplate jdbc;

    public SheetClock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LocalDate today(String sheet) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheet);
        return found.isEmpty() ? LocalDate.now() : found.get(0);
    }

    public void setToday(String sheet, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheet, Date.valueOf(today));
    }
}
