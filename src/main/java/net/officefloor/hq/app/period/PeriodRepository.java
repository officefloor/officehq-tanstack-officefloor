package net.officefloor.hq.app.period;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final String COLUMNS = "sheet_id, name, from_date, to_date, closed, closed_total, soft_closed, claim_cap, locked";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, earliest first. */
    public List<Period> findAll(String sheetId) {
        List<Period> periods = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date",
                PeriodRepository::map, sheetId);
        Map<String, Map<Long, BigDecimal>> totals = new HashMap<>();
        Map<String, Map<Long, BigDecimal>> carried = new HashMap<>();
        jdbc.query("SELECT period_name, person_id, total, carried FROM period_person_total WHERE sheet_id = ?",
                (rs) -> {
                    totals.computeIfAbsent(rs.getString("period_name"), (k) -> new HashMap<>())
                            .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                    carried.computeIfAbsent(rs.getString("period_name"), (k) -> new HashMap<>())
                            .put(rs.getLong("person_id"), rs.getBigDecimal("carried"));
                }, sheetId);
        return periods.stream().map((p) -> p.withPersonTotals(totals.getOrDefault(p.name(), Map.of()),
                carried.getOrDefault(p.name(), Map.of()))).toList();
    }

    /** The sheet's period with this name, or null. */
    public Period find(String sheetId, String name) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND name = ?",
                PeriodRepository::map, sheetId, name);
        if (found.isEmpty()) {
            return null;
        }
        Map<Long, BigDecimal> totals = new HashMap<>();
        Map<Long, BigDecimal> carried = new HashMap<>();
        jdbc.query("SELECT person_id, total, carried FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                (rs) -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                    carried.put(rs.getLong("person_id"), rs.getBigDecimal("carried"));
                }, sheetId, found.get(0).name());
        return found.get(0).withPersonTotals(totals, carried);
    }

    /** The sheet's closed period containing the date, or null. */
    public Period closedOn(String sheetId, LocalDate date) {
        return findAll(sheetId).stream().filter((p) -> p.closed() && p.contains(date)).findFirst().orElse(null);
    }

    /** The sheet's soft-closed period containing the date, or null. */
    public Period softClosedOn(String sheetId, LocalDate date) {
        return findAll(sheetId).stream().filter((p) -> p.softClosed() && p.contains(date)).findFirst().orElse(null);
    }

    /** Where each of the sheet's periods stands in the order they were added, by name (higher is later). */
    public Map<String, Long> addedOrder(String sheetId) {
        Map<String, Long> order = new HashMap<>();
        jdbc.query("SELECT name, id FROM period WHERE sheet_id = ?",
                (rs) -> {
                    order.put(rs.getString("name"), rs.getLong("id"));
                }, sheetId);
        return order;
    }

    public void insert(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, claim_cap) VALUES (?, ?, ?, ?, ?)",
                period.sheetId(), period.name(), period.from(), period.to(), period.cap());
    }

    /** Store the period's closed and soft-closed flags, fixed due-total (null when reopened) and fixed per-person due-totals and carried figures. */
    public void saveClosed(Period period) {
        jdbc.update("UPDATE period SET closed = ?, closed_total = ?, soft_closed = ? WHERE sheet_id = ? AND name = ?",
                period.closed(), period.closedTotal(), period.softClosed(), period.sheetId(), period.name());
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
        period.personTotals().forEach((personId, total) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                period.sheetId(), period.name(), personId, total,
                period.personCarried().getOrDefault(personId, BigDecimal.ZERO.setScale(2))));
    }

    /** Store the period's locked flag. */
    public void saveLocked(Period period) {
        jdbc.update("UPDATE period SET locked = ? WHERE sheet_id = ? AND name = ?", period.locked(), period.sheetId(),
                period.name());
    }

    private static Period map(ResultSet rs, int n) throws SQLException {
        return new Period(rs.getString("sheet_id"), rs.getString("name"), rs.getObject("from_date", LocalDate.class),
                rs.getObject("to_date", LocalDate.class), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"), Map.of(),
                rs.getBoolean("soft_closed"), rs.getObject("claim_cap", Integer.class), Map.of(),
                rs.getBoolean("locked"));
    }
}
