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

    /** The sheet's descendants: the sheets naming it as their parent (its children), their children,
     *  and so on, each once, nearest first. */
    public java.util.List<String> descendants(String id) {
        java.util.List<String> found = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>(java.util.List.of(id));
        for (int i = -1; i < found.size(); i++) {
            String at = i < 0 ? id : found.get(i);
            for (String child : jdbc.queryForList("SELECT sheet_id FROM sheet_setting WHERE setting = 'parent' "
                    + "AND val = ? ORDER BY sheet_id", String.class, at)) {
                if (seen.add(child)) found.add(child);
            }
        }
        return found;
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

    /** The sheet's next bulk settle batch number (1, 2, ...), taken atomically; rolled back with the request. */
    public int nextSettleBatch(String id) {
        return jdbc.queryForObject("SELECT last_settle_batch FROM FINAL TABLE (UPDATE sheet SET last_settle_batch = "
                + "COALESCE(last_settle_batch, 0) + 1 WHERE id = ?)", Integer.class, id);
    }

    /** The number of the sheet's last bulk settle batch (null: there has been none). */
    public Integer lastSettleBatch(String id) {
        return jdbc.queryForList("SELECT last_settle_batch FROM sheet WHERE id = ?", Integer.class, id)
                .stream().findFirst().orElse(null);
    }

    public Sheet find(String id) {
        return jdbc.query("SELECT id, created_by, today, archived FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getString("created_by"),
                        rs.getObject("today", LocalDate.class).toString(), rs.getBoolean("archived")), id)
                .stream().findFirst().orElse(null);
    }
}
