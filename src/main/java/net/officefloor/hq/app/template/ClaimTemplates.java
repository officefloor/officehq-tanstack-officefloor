package net.officefloor.hq.app.template;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The claim template store: each sheet's templates, by name. */
@Repository
public class ClaimTemplates {

    private static final RowMapper<ClaimTemplate> ROW = (rs, n) ->
            new ClaimTemplate(rs.getString("name"), rs.getBigDecimal("amount").setScale(2), rs.getString("note"));

    private final JdbcTemplate jdbc;

    public ClaimTemplates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates, by name. */
    public List<ClaimTemplate> list(String sheetId) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY name", ROW, sheetId);
    }

    /** The sheet's template of that name, as it is now. */
    public Optional<ClaimTemplate> find(String sheetId, String name) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? AND name = ?", ROW, sheetId, name)
                .stream().findFirst();
    }

    /** Saves the template, replacing one of the same name. */
    public void save(String sheetId, ClaimTemplate template) {
        int n = jdbc.update("UPDATE claim_template SET amount = ?, note = ? WHERE sheet_id = ? AND name = ?",
                template.amount(), template.note(), sheetId, template.name());
        if (n == 0) {
            jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) VALUES (?, ?, ?, ?)",
                    sheetId, template.name(), template.amount(), template.note());
        }
    }
}
