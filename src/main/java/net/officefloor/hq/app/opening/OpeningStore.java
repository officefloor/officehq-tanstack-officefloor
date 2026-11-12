package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each person's opening balance for a period on each sheet. */
@Repository
public class OpeningStore {

    private final JdbcTemplate jdbc;

    public OpeningStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every opening balance on the sheet, by period then person. */
    public List<OpeningResponse> list(String sheetId) {
        return jdbc.query("SELECT * FROM period_person_opening WHERE sheet_id = ? ORDER BY period_name, person_id",
                (rs, i) -> new OpeningResponse(rs.getString("period_name"), rs.getLong("person_id"),
                        rs.getBigDecimal("amount").setScale(2).toPlainString()),
                sheetId);
    }

    /** Adds the amount to the person's opening balance for the period. */
    public void add(String sheetId, String periodName, long personId, BigDecimal amount) {
        if (jdbc.update("UPDATE period_person_opening SET amount = amount + ? WHERE sheet_id = ? AND period_name = ? "
                + "AND person_id = ?", amount, sheetId, periodName, personId) == 0) {
            jdbc.update("INSERT INTO period_person_opening (sheet_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                    sheetId, periodName, personId, amount);
        }
    }
}
