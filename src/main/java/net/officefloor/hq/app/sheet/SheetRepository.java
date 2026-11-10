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
        List<Sheet> found = jdbc.query("SELECT id, today FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getDate("today").toLocalDate()), id);
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
}
