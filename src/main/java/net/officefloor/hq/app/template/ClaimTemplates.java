package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claim templates on each sheet. */
@Repository
public class ClaimTemplates {

    private final JdbcTemplate jdbc;

    public ClaimTemplates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every template on the sheet, in the order first saved. */
    public List<ClaimTemplate> list(String sheet) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet = ? ORDER BY position",
                (rs, i) -> new ClaimTemplate(rs.getString("name"), rs.getBigDecimal("amount").toPlainString(),
                        rs.getString("note")), sheet);
    }

    /** The named template, or null when the sheet has none by that name. */
    public ClaimTemplate find(String sheet, String name) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet = ? AND name = ?",
                (rs, i) -> new ClaimTemplate(rs.getString("name"), rs.getBigDecimal("amount").toPlainString(),
                        rs.getString("note")), sheet, name).stream().findFirst().orElse(null);
    }

    /** Saves the template, replacing one of the same name (which keeps its place on the list). */
    public void save(String sheet, String name, BigDecimal amount, String note) {
        int updated = jdbc.update("UPDATE claim_template SET amount = ?, note = ? WHERE sheet = ? AND name = ?",
                amount, note, sheet, name);
        if (updated == 0) {
            jdbc.update("INSERT INTO claim_template (sheet, name, amount, note, position) VALUES (?, ?, ?, ?,"
                    + " (SELECT COALESCE(MAX(p.position), 0) + 1 FROM claim_template p WHERE p.sheet = ?))",
                    sheet, name, amount, note, sheet);
        }
    }
}
