package net.officefloor.hq.app.template;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TemplateRepository {

    private static final RowMapper<ClaimTemplate> ROW = (rs, n) -> new ClaimTemplate(rs.getString("sheet_id"),
            rs.getString("name"), rs.getBigDecimal("amount"), rs.getString("note"));

    private final JdbcTemplate jdbc;

    public TemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's claim templates, by name. */
    public List<ClaimTemplate> findAll(String sheetId) {
        return jdbc.query("SELECT * FROM claim_template WHERE sheet_id = ? ORDER BY name", ROW, sheetId);
    }

    /** The sheet's template of that name, null when there is none. */
    public ClaimTemplate find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM claim_template WHERE sheet_id = ? AND name = ?", ROW, sheetId, name).stream()
                .findFirst().orElse(null);
    }

    /** Saves the template, replacing one of the same name. */
    public void save(ClaimTemplate template) {
        jdbc.update("MERGE INTO claim_template (sheet_id, name, amount, note) KEY (sheet_id, name) VALUES (?, ?, ?, ?)",
                template.sheetId(), template.name(), template.amount(), template.note());
    }
}
