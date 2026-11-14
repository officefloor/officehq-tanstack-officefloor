package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OpeningRepository {

    private final JdbcTemplate jdbc;

    public OpeningRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's opening balances added up, by period name and then person id. */
    public Map<String, Map<Long, BigDecimal>> totals(String sheetId) {
        Map<String, Map<Long, BigDecimal>> totals = new HashMap<>();
        jdbc.query("SELECT period_name, person_id, SUM(amount) AS total FROM opening_balance WHERE sheet_id = ?"
                + " GROUP BY period_name, person_id", (rs) -> {
                    totals.computeIfAbsent(rs.getString("period_name"), (k) -> new HashMap<>())
                            .put(rs.getLong("person_id"), rs.getBigDecimal("total").setScale(2));
                }, sheetId);
        return totals;
    }

    /** The period's opening balances added up, by person id. */
    public Map<Long, BigDecimal> totals(String sheetId, String periodName) {
        return totals(sheetId).getOrDefault(periodName, Map.of());
    }

    public void add(String sheetId, String periodName, long personId, BigDecimal amount) {
        jdbc.update("INSERT INTO opening_balance (sheet_id, period_name, person_id, amount) VALUES (?, ?, ?, ?)",
                sheetId, periodName, personId, amount);
    }
}
