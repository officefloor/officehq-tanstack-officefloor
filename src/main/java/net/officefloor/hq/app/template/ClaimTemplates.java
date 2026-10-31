package net.officefloor.hq.app.template;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The claim templates of each sheet, by name: saving a name again replaces its template. */
@Repository
public class ClaimTemplates {

    private final JdbcTemplate jdbc;

    public ClaimTemplates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every claim template on the sheet, by name. */
    public List<ClaimTemplate> onSheet(String sheetId) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? ORDER BY name",
                (rs, i) -> new ClaimTemplate(sheetId, rs.getString(1), rs.getBigDecimal(2), rs.getString(3), null), sheetId);
    }

    /** The sheet's template of that name, as it is now. */
    public Optional<ClaimTemplate> named(String sheetId, String name) {
        return jdbc.query("SELECT name, amount, note FROM claim_template WHERE sheet_id = ? AND name = ?",
                (rs, i) -> new ClaimTemplate(sheetId, rs.getString(1), rs.getBigDecimal(2), rs.getString(3), null),
                sheetId, name).stream().findFirst();
    }

    public void save(ClaimTemplate t) {
        jdbc.update("MERGE INTO claim_template (sheet_id, name, amount, note) KEY (sheet_id, name) VALUES (?, ?, ?, ?)",
                t.sheetId(), t.name(), t.amount(), t.note());
    }
}
