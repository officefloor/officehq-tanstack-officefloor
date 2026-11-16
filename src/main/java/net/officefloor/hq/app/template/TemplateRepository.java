package net.officefloor.hq.app.template;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored claim templates, kept per sheet by name. */
@Repository
public class TemplateRepository {

    private final JdbcTemplate jdbc;

    public TemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates in the order first saved. */
    public List<Template> list(String sheetId) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY id",
                (rs, n) -> new Template(rs.getString("name"), rs.getBigDecimal("amount").setScale(2), rs.getString("note")), sheetId);
    }

    /** The sheet's template of the name; null when there is none. */
    public Template find(String sheetId, String name) {
        List<Template> found = jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? AND name = ?",
                (rs, n) -> new Template(rs.getString("name"), rs.getBigDecimal("amount").setScale(2), rs.getString("note")),
                sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Stores the template, replacing the amount and note of one already saved under its name. */
    public void save(String sheetId, Template template) {
        int updated = jdbc.update("UPDATE claim_template SET amount = ?, note = ? WHERE sheet_id = ? AND name = ?",
                template.amount(), template.note(), sheetId, template.name());
        if (updated == 0) {
            jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) VALUES (?, ?, ?, ?)", sheetId,
                    template.name(), template.amount(), template.note());
        }
    }
}
