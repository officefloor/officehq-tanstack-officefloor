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
        return new Sheet(id, today(id));
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

    public void saveToday(String id, LocalDate today) {
        jdbc.update("MERGE INTO sheet (id, today) KEY (id) VALUES (?, ?)", id, today);
    }
}
