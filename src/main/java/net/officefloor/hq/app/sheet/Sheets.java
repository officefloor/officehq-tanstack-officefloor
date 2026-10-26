package net.officefloor.hq.app.sheet;

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
        List<Sheet> found = jdbc.query("SELECT today_date, archived FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(id, rs.getObject(1, LocalDate.class), rs.getBoolean(2)), id);
        return found.isEmpty() ? new Sheet(id, LocalDate.now(), false) : found.get(0);
    }

    /** The sheet's today: use this, never the real clock, for anything that depends on the date. */
    public LocalDate today(String id) {
        return find(id).today();
    }

    /** Whether the sheet is archived, and so refuses every change. */
    public boolean isArchived(String id) {
        return find(id).archived();
    }

    public void save(Sheet sheet) {
        jdbc.update("MERGE INTO sheet (id, today_date, archived) KEY (id) VALUES (?, ?, ?)", sheet.id(), sheet.today(),
                sheet.archived());
    }
}
