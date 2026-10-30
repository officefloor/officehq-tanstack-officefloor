package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The opening store: amounts pasted for a person and a period, kept per sheet; a person's opening for a
 *  period is the sum of theirs for it. */
@Repository
public class Openings {

    private final JdbcTemplate jdbc;

    public Openings(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Adds the amount to the person's opening for the period. */
    public void add(String sheetId, long personId, String period, BigDecimal amount) {
        jdbc.update("INSERT INTO period_opening (sheet_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                sheetId, period, personId, amount);
    }

    /** Each person's opening for each period on the sheet that has one. */
    public List<OpeningResponse> list(String sheetId) {
        return jdbc.query("SELECT person_id, period_name, SUM(amount) AS amount FROM period_opening WHERE sheet_id = ?"
                + " GROUP BY person_id, period_name ORDER BY person_id, period_name",
                (rs, i) -> new OpeningResponse(String.valueOf(rs.getLong("person_id")), rs.getString("period_name"),
                        rs.getBigDecimal("amount").setScale(2).toPlainString()), sheetId);
    }

    /** Each person's opening for the period, by person id; a person with none is absent. */
    public Map<Long, BigDecimal> forPeriod(String sheetId, String period) {
        Map<Long, BigDecimal> found = new HashMap<>();
        jdbc.query("SELECT person_id, SUM(amount) AS amount FROM period_opening WHERE sheet_id = ? AND period_name = ?"
                + " GROUP BY person_id", rs -> {
                    found.put(rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2));
                }, sheetId, period);
        return found;
    }
}
