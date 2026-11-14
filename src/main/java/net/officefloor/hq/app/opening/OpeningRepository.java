package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    /** The people whose opening balance for the period was set by a rollover still standing (not undone). */
    public Set<Long> carriedByRollover(String sheetId, String periodName) {
        return new HashSet<>(jdbc.queryForList("SELECT o.person_id FROM opening_balance o JOIN rollover r ON r.id = o.rollover_id"
                + " WHERE o.sheet_id = ? AND o.period_name = ? AND r.undone = FALSE", Long.class, sheetId, periodName));
    }

    /** Add an opening balance set by a rollover, carrying its id so an undo can take it back. */
    public void addFromRollover(String sheetId, String periodName, long personId, BigDecimal amount, long rolloverId) {
        jdbc.update("INSERT INTO opening_balance (sheet_id, period_name, person_id, amount, rollover_id) VALUES (?, ?, ?, ?, ?)",
                sheetId, periodName, personId, amount, rolloverId);
    }
}
