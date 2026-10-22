package net.officefloor.hq.app.sheet;

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

    /** Create the sheet unless it already exists (concurrent first loads are harmless). */
    public void ensure(String id, String user) {
        jdbc.update("MERGE INTO sheet (id, created_by) KEY (id) VALUES (?, ?)", id, user);
    }
}
