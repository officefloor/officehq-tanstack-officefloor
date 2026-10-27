package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.history.AsOf;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of sheets. A sheet never stored is an empty sheet whose today is the real date. */
@Repository
public class Sheets {

    private final JdbcTemplate jdbc;

    public Sheets(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Sheet find(String id) {
        // While the store is read as of a date, that date stands in for the sheet's today.
        LocalDate asOf = AsOf.date();
        List<Sheet> found = jdbc.query("SELECT today_date, archived FROM " + AsOf.table("sheet") + " WHERE id = ?",
                (rs, n) -> new Sheet(id, asOf != null ? asOf : rs.getObject(1, LocalDate.class), rs.getBoolean(2)), id);
        return found.isEmpty() ? new Sheet(id, asOf != null ? asOf : LocalDate.now(), false) : found.get(0);
    }

    /** The sheet's today: use this, never the real clock, for anything that depends on the date. */
    public LocalDate today(String id) {
        return find(id).today();
    }

    /** Whether the sheet is archived, and so refuses every change. */
    public boolean isArchived(String id) {
        return find(id).archived();
    }

    /** Keeps the id as a sheet that has been loaded. */
    public void markLoaded(String id) {
        jdbc.update("MERGE INTO sheet_loaded (id) KEY (id) VALUES (?)", id);
    }

    /** Whether the sheet has been loaded (or changed) before. */
    public boolean exists(String id) {
        Integer n = jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM sheet_loaded WHERE id = ?) + (SELECT COUNT(*) FROM sheet WHERE id = ?)",
                Integer.class, id, id);
        return n != null && n > 0;
    }

    public void save(Sheet sheet) {
        jdbc.update("MERGE INTO sheet (id, today_date, archived) KEY (id) VALUES (?, ?, ?)", sheet.id(), sheet.today(),
                sheet.archived());
    }
}
