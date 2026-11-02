package net.officefloor.hq.app.sheet;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, made empty when the id was never used. */
    public Sheet loadOrCreate(String id, String user) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, id);
        if (known == null || known == 0) {
            try {
                jdbc.update("INSERT INTO sheet (id, created_by) VALUES (?, ?)", id, user);
            } catch (DuplicateKeyException made) {
                // another user loaded the same new id at the same moment: it is made either way
            }
        }
        return new Sheet(id);
    }
}
