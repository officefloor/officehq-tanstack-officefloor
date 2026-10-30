package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The sheet store. {@link #today(String)} is THE date for a sheet: every feature that decides
 * anything by date, or fills in a default date, asks it — never {@code LocalDate.now()}.
 */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today; a sheet seen for the first time starts at the real date, kept from then on. */
    public LocalDate today(String sheetId) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        LocalDate now = LocalDate.now();
        try {
            jdbc.update("INSERT INTO sheet (id, today) VALUES (?, ?)", sheetId, Date.valueOf(now));
        } catch (DuplicateKeyException raced) {
            // another request started this sheet first; its date stands
        }
        return jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate(1).toLocalDate(), sheetId);
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }
}
