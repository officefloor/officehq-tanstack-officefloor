package net.officefloor.hq.app.sheet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheets that exist: every sheet id that has been loaded. */
@Repository
public class SheetRegistry {

    private final JdbcTemplate jdbc;

    public SheetRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Records that the sheet has been loaded. */
    public void loaded(String sheet) {
        jdbc.update("MERGE INTO sheet_loaded (id) KEY (id) VALUES (?)", sheet);
    }

    /** Whether the sheet has been loaded. */
    public boolean exists(String sheet) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sheet_loaded WHERE id = ?", Integer.class, sheet) > 0;
    }
}
