package net.officefloor.hq.app.template;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claim templates of each sheet, by name, each name at most once on its sheet. */
@Repository
public class TemplateRepository {

    private final JdbcTemplate jdbc;

    public TemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates, by name. */
    public List<Template> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY name",
                TemplateRepository::map, sheetId);
    }

    /** The sheet's template of that name; null when there is none. */
    public Template find(String sheetId, String name) {
        return jdbc.query("SELECT sheet_id, name, amount, note FROM claim_template WHERE sheet_id = ? AND name = ?",
                TemplateRepository::map, sheetId, name).stream().findFirst().orElse(null);
    }

    /** Saves the template, replacing one of the same name on its sheet. */
    public void save(Template t) {
        int n = jdbc.update("UPDATE claim_template SET amount = ?, note = ? WHERE sheet_id = ? AND name = ?",
                t.amount(), t.note(), t.sheetId(), t.name());
        if (n == 0) {
            jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) VALUES (?, ?, ?, ?)",
                    t.sheetId(), t.name(), t.amount(), t.note());
        }
    }

    private static Template map(ResultSet rs, int n) throws SQLException {
        return new Template(rs.getString("sheet_id"), rs.getString("name"), rs.getBigDecimal("amount").setScale(2),
                rs.getString("note"));
    }
}
