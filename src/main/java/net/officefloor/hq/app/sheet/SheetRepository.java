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

    /** The sheet with this id, made (empty) if it has never been loaded before. */
    public Sheet open(String id, String user) {
        try {
            jdbc.update("INSERT INTO sheet (id, created_by) "
                    + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)", id, user, id);
        } catch (DuplicateKeyException alreadyMade) {
            // another user made it at the same moment: it is there now
        }
        return jdbc.queryForObject("SELECT id, created_by FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getString("created_by")), id);
    }
}
