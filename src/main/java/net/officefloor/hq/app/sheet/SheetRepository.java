package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Sheets by id. A sheet is created the first time its id is loaded, and starts empty. */
@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean exists(String id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, id);
        return n != null && n > 0;
    }

    /**
     * Create the sheet unless it already exists (concurrent first loads are harmless). A new sheet's
     * today starts as the real date — the only place the app reads the real clock.
     */
    public void ensure(String id, String user) {
        jdbc.update("MERGE INTO sheet s USING (VALUES (CAST(? AS VARCHAR(100)), CAST(? AS VARCHAR(100)), CAST(? AS DATE))) "
                + "v(id, created_by, today) ON s.id = v.id "
                + "WHEN NOT MATCHED THEN INSERT (id, created_by, today) VALUES (v.id, v.created_by, v.today)",
                id, user, LocalDate.now());
    }

    /** The sheet's today: the date every date-dependent decision on the sheet uses. Null when absent. */
    public LocalDate today(String id) {
        return jdbc.query("SELECT today FROM sheet WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null, id);
    }

    public void setToday(String id, LocalDate today) {
        jdbc.update("UPDATE sheet SET today = ? WHERE id = ?", today, id);
    }
}
