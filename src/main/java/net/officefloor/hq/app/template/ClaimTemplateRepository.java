package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimTemplateRepository {

    private static final RowMapper<ClaimTemplate> ROW = (rs, n) -> new ClaimTemplate(rs.getString("sheet_id"),
            rs.getString("name"), rs.getBigDecimal("amount").setScale(2), rs.getString("note"), rs.getString("saved_by"));

    private final JdbcTemplate jdbc;

    public ClaimTemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates, by name. */
    public List<ClaimTemplate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, name, amount, note, saved_by FROM claim_template WHERE sheet_id = ? ORDER BY name",
                ROW, sheetId);
    }

    /** The sheet's template of that name, or null. */
    public ClaimTemplate find(String sheetId, String name) {
        List<ClaimTemplate> found = jdbc.query(
                "SELECT sheet_id, name, amount, note, saved_by FROM claim_template WHERE sheet_id = ? AND name = ?",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Saves the template, replacing one of the same name. */
    public ClaimTemplate save(String sheetId, String name, BigDecimal amount, String note, String by) {
        int n = jdbc.update("UPDATE claim_template SET amount = ?, note = ?, saved_by = ? WHERE sheet_id = ? AND name = ?",
                amount, note, by, sheetId, name);
        if (n == 0) {
            jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note, saved_by) VALUES (?, ?, ?, ?, ?)",
                    sheetId, name, amount, note, by);
        }
        return new ClaimTemplate(sheetId, name, amount, note, by);
    }
}
