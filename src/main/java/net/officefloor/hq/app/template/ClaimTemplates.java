package net.officefloor.hq.app.template;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of the sheet's claim templates, each name at most once per sheet. */
@Repository
public class ClaimTemplates {

    private final JdbcTemplate jdbc;

    public ClaimTemplates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's templates, in the order they were first saved. */
    public List<ClaimTemplate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY listed",
                (rs, n) -> new ClaimTemplate(rs.getString("sheet_id"), rs.getString("name"),
                        rs.getBigDecimal("amount").setScale(2), rs.getString("note")),
                sheetId);
    }

    /** The template of that name; null when the sheet has none. */
    public ClaimTemplate find(String sheetId, String name) {
        return list(sheetId).stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }

    /** Saves the template, replacing the amount and note of one already saved under its name. */
    public void save(ClaimTemplate template) {
        int updated = jdbc.update("UPDATE claim_template SET amount = ?, note = ? WHERE sheet_id = ? AND name = ?",
                template.amount(), template.note(), template.sheetId(), template.name());
        if (updated == 0) {
            jdbc.update("INSERT INTO claim_template (sheet_id, name, amount, note, listed) VALUES (?, ?, ?, ?, "
                    + "(SELECT COALESCE(MAX(listed), 0) + 1 FROM claim_template WHERE sheet_id = ?))",
                    template.sheetId(), template.name(), template.amount(), template.note(), template.sheetId());
        }
    }
}
