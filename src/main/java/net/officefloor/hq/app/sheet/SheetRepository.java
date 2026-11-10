package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet store. */
@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, created empty (its today the real date) when never loaded before. */
    public Sheet loadOrCreate(String id) {
        jdbc.update("MERGE INTO sheet USING (VALUES (CAST(? AS VARCHAR(200)), CAST(? AS DATE))) s(id, today)"
                + " ON sheet.id = s.id WHEN NOT MATCHED THEN INSERT (id, today) VALUES (s.id, s.today)",
                id, Date.valueOf(LocalDate.now()));
        return find(id);
    }

    /** The sheet with this id, or null when it was never loaded. */
    public Sheet find(String id) {
        List<Sheet> found = jdbc.query("SELECT id, today, archived FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getDate("today").toLocalDate(),
                        rs.getBoolean("archived")), id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet's today: the date the sheet uses instead of the real clock. */
    public LocalDate today(String id) {
        Sheet sheet = find(id);
        return sheet == null ? LocalDate.now() : sheet.today();
    }

    public void saveToday(String id, LocalDate today) {
        jdbc.update("UPDATE sheet SET today = ? WHERE id = ?", Date.valueOf(today), id);
    }

    /** Archive the sheet: true when this call archived it, false when it already was. */
    public boolean archive(String id) {
        return jdbc.update("UPDATE sheet SET archived = TRUE, archived_on = today WHERE id = ? AND archived = FALSE", id) > 0;
    }

    /**
     * The sheet as it stood at the end of {@code day}: that date stands in for its today, and it is
     * archived only when it was archived while its today was on or before it.
     */
    public Sheet asOf(Sheet sheet, LocalDate day) {
        List<Boolean> archived = jdbc.query("SELECT archived AND (archived_on IS NULL OR archived_on <= ?) AS archived_then"
                + " FROM sheet WHERE id = ?", (rs, n) -> rs.getBoolean("archived_then"), Date.valueOf(day), sheet.id());
        return new Sheet(sheet.id(), day, !archived.isEmpty() && archived.get(0));
    }

    /** The sheet's parent sheet's id, or null when it has none (or was never loaded). */
    public String parent(String id) {
        List<String> found = jdbc.query("SELECT parent_id FROM sheet WHERE id = ?",
                (rs, n) -> rs.getString("parent_id"), id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The ids of the sheets naming this one as their parent, in id order. */
    public List<String> children(String id) {
        return jdbc.query("SELECT id FROM sheet WHERE parent_id = ? ORDER BY id", (rs, n) -> rs.getString("id"), id);
    }

    /** Names the sheet's parent sheet (null: none). */
    public void saveParent(String id, String parent) {
        jdbc.update("UPDATE sheet SET parent_id = ? WHERE id = ?", parent, id);
    }

    /** Whether the sheet has been archived; false for a sheet never loaded. */
    public boolean isArchived(String id) {
        Sheet sheet = find(id);
        return sheet != null && sheet.archived();
    }
}
