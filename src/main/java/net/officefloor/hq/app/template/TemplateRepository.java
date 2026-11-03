package net.officefloor.hq.app.template;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TemplateRepository {

    private final JdbcTemplate jdbc;

    public TemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's templates, by name. */
    public List<Template> list(String sheetId) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY name",
                (rs, i) -> new Template(rs.getString("name"), rs.getBigDecimal("amount").setScale(2).toPlainString(),
                        rs.getString("note")), sheetId);
    }

    /** The sheet's template of that name, or null when there is none. */
    public Template find(String sheetId, String name) {
        return list(sheetId).stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }

    /** Save a template, replacing the one of the same name. */
    public Template save(String sheetId, String name, BigDecimal amount, String note) {
        jdbc.update("DELETE FROM claim_template WHERE sheet_id = ? AND name = ?", sheetId, name);
        jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note) VALUES (?, ?, ?, ?)",
                sheetId, name, amount, note);
        return new Template(name, amount.setScale(2).toPlainString(), note);
    }
}
