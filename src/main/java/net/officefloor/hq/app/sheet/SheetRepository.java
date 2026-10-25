package net.officefloor.hq.app.sheet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheets: a row per id ever loaded. */
@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Create the sheet if its id was never used; an existing sheet is left as it is. */
    public void ensure(String id) {
        jdbc.update("MERGE INTO sheet (id) KEY (id) VALUES (?)", id);
    }
}
