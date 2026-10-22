package net.officefloor.hq.app.template;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The claim templates by sheet. */
@Repository
public class TemplateRepository {

    private static final RowMapper<Template> ROW = (rs, i) -> new Template(rs.getString("sheet_id"),
            rs.getString("name"), rs.getBigDecimal("amount"), rs.getString("note"), rs.getString("saved_by"));

    private final JdbcTemplate jdbc;

    public TemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates, by name. */
    public List<Template> list(String sheetId) {
        return jdbc.query("SELECT * FROM claim_template WHERE sheet_id = ? ORDER BY name", ROW, sheetId);
    }

    /** The sheet's template of that name, null when it has none. */
    public Template find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM claim_template WHERE sheet_id = ? AND name = ?", ROW, sheetId, name)
                .stream().findFirst().orElse(null);
    }

    /** Store the template, replacing the sheet's template of the same name. */
    public Template save(String sheetId, TemplateInput input) {
        jdbc.update("DELETE FROM claim_template WHERE sheet_id = ? AND name = ?", sheetId, input.name());
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note, saved_by) VALUES (?, ?, ?, ?, ?)",
                sheetId, input.name(), input.amount(), input.note(), input.user());
        return new Template(sheetId, input.name(), input.amount(), input.note(), input.user());
    }
}
