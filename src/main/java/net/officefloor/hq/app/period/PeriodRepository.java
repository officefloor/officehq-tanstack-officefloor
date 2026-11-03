package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getLong("id"), rs.getString("name"),
            rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total") == null ? null
                    : rs.getBigDecimal("closed_total").setScale(2).toPlainString(), Map.of(), Map.of(),
            rs.getBoolean("soft_closed"), (Integer) rs.getObject("claim_cap", Integer.class));

    private static final String COLUMNS = "id, name, from_date, to_date, closed, closed_total, soft_closed, claim_cap";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lock the sheet's row until the transaction commits, so two periods added at once are checked
     *  one after the other; false when there is no such sheet. */
    public boolean lockSheet(String sheetId) {
        return !jdbc.queryForList("SELECT id FROM sheet WHERE id = ? FOR UPDATE", String.class, sheetId).isEmpty();
    }

    /** The sheet's today (yyyy-MM-dd); null when the sheet has none. */
    public String today(String sheetId) {
        List<Date> found = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate().toString();
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        List<Period> periods = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date",
                ROW, sheetId);
        Map<Long, Map<String, String>> totals = new HashMap<>();
        Map<Long, Map<String, String>> carried = new HashMap<>();
        jdbc.query("SELECT period_id, person_id, total, carried FROM period_person_total WHERE sheet_id = ? ORDER BY person_id",
                rs -> {
                    totals.computeIfAbsent(rs.getLong("period_id"), k -> new LinkedHashMap<>())
                            .put(Long.toString(rs.getLong("person_id")), rs.getBigDecimal("total").setScale(2).toPlainString());
                    carried.computeIfAbsent(rs.getLong("period_id"), k -> new LinkedHashMap<>())
                            .put(Long.toString(rs.getLong("person_id")), rs.getBigDecimal("carried").setScale(2).toPlainString());
                }, sheetId);
        return periods.stream().map(p -> new Period(p.id(), p.name(), p.from(), p.to(), p.closed(), p.closedTotal(),
                totals.getOrDefault(p.id(), Map.of()), carried.getOrDefault(p.id(), Map.of()), p.softClosed(), p.cap())).toList();
    }

    /** The sheet's period of this name (exactly); null when there is none. */
    public Period find(String sheetId, String name) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND name = ?", ROW,
                sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The name of the sheet's closed period whose dates contain a date; null when none does. */
    public String closedOn(String sheetId, LocalDate on) {
        List<String> found = jdbc.queryForList("SELECT name FROM period WHERE sheet_id = ? AND closed"
                + " AND from_date <= ? AND to_date >= ?", String.class, sheetId, Date.valueOf(on), Date.valueOf(on));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Close (or soft-close) a period, fixing afresh its due-total and each person's due-total and carried figure for
     *  it (by person id). */
    public void close(String sheetId, long periodId, BigDecimal total, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried, boolean soft) {
        jdbc.update("DELETE FROM period_person_total WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
        jdbc.update("UPDATE period SET closed = ?, soft_closed = ?, closed_total = ? WHERE id = ? AND sheet_id = ?",
                !soft, soft, total, periodId, sheetId);
        for (Map.Entry<Long, BigDecimal> e : personTotals.entrySet()) {
            jdbc.update("INSERT INTO period_person_total (period_id, sheet_id, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                    periodId, sheetId, e.getKey(), e.getValue(), personCarried.getOrDefault(e.getKey(), BigDecimal.ZERO));
        }
    }

    /** Reopen a closed or soft-closed period, dropping its fixed due-total and figures. */
    public void reopen(String sheetId, long periodId) {
        jdbc.update("DELETE FROM period_person_total WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
        jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL WHERE id = ? AND sheet_id = ?", periodId,
                sheetId);
    }

    /** Whether the sheet has a period of this name, without regard to case. */
    public boolean named(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name_key = LOWER(?)",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether any period of the sheet shares a day with from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ?"
                + " AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    /** The sheet's period whose dates contain a date; null when none does. */
    public Period containing(String sheetId, LocalDate on) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                ROW, sheetId, Date.valueOf(on), Date.valueOf(on));
        return found.isEmpty() ? null : found.get(0);
    }

    /** How many OPEN or PENDING claims a person has whose due-by falls in a period. */
    public int openIn(String sheetId, long personId, Period period) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND status IN ('OPEN', 'PENDING') AND due_by >= ? AND due_by <= ?", Integer.class, sheetId, personId,
                Date.valueOf(LocalDate.parse(period.from())), Date.valueOf(LocalDate.parse(period.to())));
        return n == null ? 0 : n;
    }

    public Period add(String sheetId, String name, LocalDate from, LocalDate to) {
        return add(sheetId, name, from, to, null);
    }

    public Period add(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO period (sheet_id, name, from_date, to_date, claim_cap) VALUES (?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            if (cap == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, cap);
            }
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), name, from.toString(), to.toString(), false, null, Map.of(), Map.of(), false, cap);
    }
}
