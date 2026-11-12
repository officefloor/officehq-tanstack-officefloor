package net.officefloor.hq.app.template;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The claim templates per sheet, one per name. */
@Repository
public class TemplateStore {

    private static final RowMapper<Template> ROW = (rs, n) -> new Template(rs.getString("name"),
            rs.getBigDecimal("amount").setScale(2), rs.getString("note"));

    private final JdbcTemplate jdbc;

    public TemplateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates, by name. */
    public List<Template> list(String sheetId) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY name", ROW, sheetId);
    }

    /** The sheet's templates at the end of the date (see {@link History}), by name. */
    public List<Template> listAsOf(String sheetId, LocalDate date) {
        return jdbc.query("SELECT name, amount, note FROM " + History.asOf("claim_template", "id", date)
                + " WHERE sheet_id = ? ORDER BY name", ROW, sheetId);
    }

    /** The template of that name, as it is now; null when the sheet has none. */
    public Template find(String sheetId, String name) {
        List<Template> found = jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? AND name = ?",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Saves the template, replacing the amount and note of one already saved under its name. */
    public void save(String sheetId, Template t) {
        if (jdbc.update("UPDATE claim_template SET amount = ?, note = ? WHERE sheet_id = ? AND name = ?",
                t.amount(), t.note(), sheetId, t.name()) == 0) {
            jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) VALUES (?, ?, ?, ?)",
                    sheetId, t.name(), t.amount(), t.note());
        }
    }
}
