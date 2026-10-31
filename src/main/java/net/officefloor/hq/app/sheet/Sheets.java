package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The sheets and their "today". A sheet starts with the real date the first time it is loaded;
 * from then on {@link #today(String)} is the only date the app uses for that sheet — never the
 * real clock.
 */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's today, starting the sheet at the real date when it has never been used. */
    public LocalDate today(String sheetId) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) SELECT ?, ? FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)",
                sheetId, Date.valueOf(LocalDate.now()), sheetId);
        return jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId)
                .toLocalDate();
    }

    public void setToday(String sheetId, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", sheetId, Date.valueOf(today));
    }
}
