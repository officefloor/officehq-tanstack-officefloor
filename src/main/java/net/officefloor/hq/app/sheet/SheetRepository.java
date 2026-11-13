package net.officefloor.hq.app.sheet;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SheetRepository {

    private final JdbcTemplate jdbc;

    public SheetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet with this id, created empty (today = the real date) when it was never loaded before. */
    public Sheet ensure(String id) {
        jdbc.update("INSERT INTO sheet (id, today) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM sheet WHERE id = ?)",
                id, Date.valueOf(LocalDate.now()), id);
        return find(id);
    }

    /** The sheet with this id, or null when it was never loaded. */
    public Sheet find(String id) {
        List<Sheet> found = jdbc.query("SELECT id, today FROM sheet WHERE id = ?",
                (rs, n) -> new Sheet(rs.getString("id"), rs.getDate("today").toLocalDate()), id);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The sheet's today. Use this — never {@code LocalDate.now()} — for any date-dependent decision
     * or default date in the sheet.
     */
    public LocalDate today(String id) {
        Sheet sheet = find(id);
        return sheet == null ? LocalDate.now() : sheet.today();
    }

    /**
     * Takes the sheet's next claim number (1 for its first) in one atomic statement, so two claims
     * opened at the same moment never get the same number; a number taken is never given out again.
     */
    public int nextClaimNumber(String id) {
        return jdbc.queryForObject("SELECT last_claim_number FROM FINAL TABLE "
                + "(UPDATE sheet SET last_claim_number = last_claim_number + 1 WHERE id = ?)", Integer.class, id);
    }

    public void save(Sheet sheet) {
        jdbc.update("UPDATE sheet SET today = ? WHERE id = ?", Date.valueOf(sheet.today()), sheet.id());
    }
}
