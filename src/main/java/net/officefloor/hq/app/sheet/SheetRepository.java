package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored sheets. A sheet never changed has no row and reads as starting on the real date. */
@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Sheet find(String id) {
        return new Sheet(id, today(id), archived(id));
    }

    /** The sheet as it stood at the end of the date: the date stands in for its today, and it is archived only when it
     *  was archived while its today was on or before it. */
    public Sheet find(String id, LocalDate asOf) {
        if (asOf == null) {
            return find(id);
        }
        List<LocalDate> archivedOn = jdbc.query("SELECT archived_on FROM sheet_archived WHERE sheet_id = ?",
                (rs, n) -> rs.getObject(1, LocalDate.class), id);
        return new Sheet(id, asOf, !archivedOn.isEmpty() && AsOf.counts(archivedOn.get(0), asOf));
    }

    /**
     * The sheet's today. Use this — never {@code LocalDate.now()} — for every decision or default
     * that depends on the date.
     */
    public LocalDate today(String id) {
        List<LocalDate> found = jdbc.query("SELECT today FROM sheet WHERE id = ?",
                (rs, n) -> rs.getObject(1, LocalDate.class), id);
        return found.isEmpty() ? LocalDate.now() : found.get(0);
    }

    /** Whether a steward archived the sheet: an archived sheet refuses every change. */
    public boolean archived(String id) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM sheet_archived WHERE sheet_id = ?", Integer.class, id);
        return found != null && found > 0;
    }

    /** Archives the sheet; false when it was already archived. */
    public boolean archive(String id, String by) {
        if (archived(id)) {
            return false;
        }
        jdbc.update("INSERT INTO sheet_archived (sheet_id, archived_by, archived_on) VALUES (?, ?, ?)", id, by, today(id));
        return true;
    }

    public void saveToday(String id, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", id, today);
    }
}
