package net.officefloor.hq.app.sheet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet store. */
@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, created empty when it was never loaded before. */
    public Sheet loadOrCreate(String id) {
        jdbc.update("MERGE INTO sheet (id) KEY (id) VALUES (?)", id);
        return new Sheet(id);
    }
}
