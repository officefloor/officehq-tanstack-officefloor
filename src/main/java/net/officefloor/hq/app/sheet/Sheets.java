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
        List<LocalDate> today = jdbc.query("SELECT today_date FROM sheet WHERE id = ?",
                (rs, n) -> rs.getObject(1, LocalDate.class), id);
        return new Sheet(id, today.isEmpty() ? LocalDate.now() : today.get(0));
    }

    /** The sheet's today: use this, never the real clock, for anything that depends on the date. */
    public LocalDate today(String id) {
        return find(id).today();
    }

    public void save(Sheet sheet) {
        jdbc.update("MERGE INTO sheet (id, today_date) KEY (id) VALUES (?, ?)", sheet.id(), sheet.today());
    }
}
