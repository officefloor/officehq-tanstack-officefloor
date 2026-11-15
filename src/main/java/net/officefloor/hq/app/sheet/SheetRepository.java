package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, made (empty, today the real date) if it has never been loaded before. */
    public Sheet open(String id, String user) {
        try {
            jdbc.update("INSERT INTO sheet (id, created_by, today) "
                    + "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)",
                    id, user, LocalDate.now(), id);
        } catch (DuplicateKeyException alreadyMade) {
            // another user made it at the same moment: it is there now
        }
        return find(id);
    }

    /** The sheet's today: the date every date decision and default on the sheet uses. */
    public LocalDate today(String id) {
        Sheet sheet = find(id);
        return sheet == null ? LocalDate.now() : LocalDate.parse(sheet.today());
    }

    public void setToday(String id, LocalDate today) {
        jdbc.update("UPDATE sheet SET today = ? WHERE id = ?", today, id);
    }

    /** Whether a steward has archived the sheet (a sheet never loaded is not). */
    public boolean isArchived(String id) {
        Sheet sheet = find(id);
        return sheet != null && sheet.archived();
    }

    public void archive(String id) {
        jdbc.update("UPDATE sheet SET archived = TRUE WHERE id = ?", id);
    }

    public Sheet find(String id) {
        return jdbc.query("SELECT id, created_by, today, archived FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getString("created_by"),
                        rs.getObject("today", LocalDate.class).toString(), rs.getBoolean("archived")), id)
                .stream().findFirst().orElse(null);
    }
}
