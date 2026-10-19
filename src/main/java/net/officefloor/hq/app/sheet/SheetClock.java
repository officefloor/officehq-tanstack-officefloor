package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Each sheet's "today". It starts as the real date and is changed from the screen. Every decision
 * that depends on the date, and every default date, uses {@link #today(String)} — never the real
 * clock.
 */
@Service
public class SheetClock {

    private final JdbcTemplate jdbc;

    public SheetClock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today: the stored date, or the real date when none has been set. */
    public LocalDate today(String sheetId) {
        List<Date> stored = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId);
        Date today = stored.isEmpty() ? null : stored.get(0);
        return today == null ? LocalDate.now() : today.toLocalDate();
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }
}
