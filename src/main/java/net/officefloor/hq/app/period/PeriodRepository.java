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
            rs.getBoolean("soft_closed"), (Integer) rs.getObject("claim_cap", Integer.class), Map.of(),
            rs.getBoolean("locked"));

    private static final String COLUMNS = "id, name, from_date, to_date, closed, closed_total, soft_closed, claim_cap, locked";

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
        Map<Long, Map<String, String>> openings = new HashMap<>();
        jdbc.query("SELECT period_id, person_id, amount FROM period_opening WHERE sheet_id = ? ORDER BY person_id",
                rs -> {
                    openings.computeIfAbsent(rs.getLong("period_id"), k -> new LinkedHashMap<>())
                            .put(Long.toString(rs.getLong("person_id")), rs.getBigDecimal("amount").setScale(2).toPlainString());
                }, sheetId);
        return periods.stream().map(p -> new Period(p.id(), p.name(), p.from(), p.to(), p.closed(), p.closedTotal(),
                totals.getOrDefault(p.id(), Map.of()), carried.getOrDefault(p.id(), Map.of()), p.softClosed(), p.cap(),
                openings.getOrDefault(p.id(), Map.of()), p.locked())).toList();
    }

    /** Each person's opening balance for a period, by person id (only those with one). */
    public Map<Long, BigDecimal> openings(String sheetId, long periodId) {
        Map<Long, BigDecimal> found = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, amount FROM period_opening WHERE sheet_id = ? AND period_id = ? ORDER BY person_id",
                rs -> {
                    found.put(rs.getLong("person_id"), rs.getBigDecimal("amount"));
                }, sheetId, periodId);
        return found;
    }

    /** Add an amount to a person's opening balance for a period. */
    public void addOpening(String sheetId, long periodId, long personId, BigDecimal amount) {
        int changed = jdbc.update("UPDATE period_opening SET amount = amount + ? WHERE sheet_id = ? AND period_id = ? AND person_id = ?",
                amount, sheetId, periodId, personId);
        if (changed == 0) {
            jdbc.update("INSERT INTO period_opening (period_id, sheet_id, person_id, amount) VALUES (?, ?, ?, ?)",
                    periodId, sheetId, personId, amount);
        }
    }

    /** Keep a rollover as the sheet's last one, replacing any kept before: who made it, on which sheet-today, for
     *  which fiscal year, the periods it generated (by id) and the opening balances it added (period id, person id,
     *  amount). */
    public void keepRollover(String sheetId, String by, String today, String fy, List<Long> periodIds,
            List<Object[]> openings) {
        forgetRollover(sheetId);
        jdbc.update("INSERT INTO rollover_last (sheet_id, by_user, made_on, fy) VALUES (?, ?, ?, ?)", sheetId, by,
                today == null ? null : Date.valueOf(LocalDate.parse(today)), fy);
        for (Long periodId : periodIds) {
            jdbc.update("INSERT INTO rollover_last_period (sheet_id, period_id) VALUES (?, ?)", sheetId, periodId);
        }
        for (Object[] o : openings) {
            jdbc.update("INSERT INTO rollover_last_opening (sheet_id, period_id, person_id, amount) VALUES (?, ?, ?, ?)",
                    sheetId, o[0], o[1], o[2]);
        }
    }

    /** The sheet's last rollover kept for undoing: by, made-on (yyyy-MM-dd or null) and fy; null when none. */
    public String[] lastRollover(String sheetId) {
        List<String[]> found = jdbc.query("SELECT by_user, made_on, fy FROM rollover_last WHERE sheet_id = ?",
                (rs, i) -> new String[] { rs.getString("by_user"),
                        rs.getDate("made_on") == null ? null : rs.getDate("made_on").toLocalDate().toString(),
                        rs.getString("fy") }, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The ids of the periods the sheet's last rollover generated. */
    public List<Long> lastRolloverPeriods(String sheetId) {
        return jdbc.queryForList("SELECT period_id FROM rollover_last_period WHERE sheet_id = ? ORDER BY period_id",
                Long.class, sheetId);
    }

    /** Whether any claim of the sheet (in any status) belongs to one of these periods, by its date or its due-by. */
    public boolean claimsIn(String sheetId, List<Long> periodIds) {
        for (Long periodId : periodIds) {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim c JOIN period p ON p.sheet_id = c.sheet_id"
                    + " WHERE c.sheet_id = ? AND p.id = ? AND ((c.claim_date >= p.from_date AND c.claim_date <= p.to_date)"
                    + " OR (c.due_by >= p.from_date AND c.due_by <= p.to_date))", Integer.class, sheetId, periodId);
            if (n != null && n > 0) return true;
        }
        return false;
    }

    /** Undo the sheet's last rollover: take back the opening balances it added (dropping one that comes to 0.00 or
     *  less), remove the periods it generated with everything fixed for them, and forget it. */
    public void undoRollover(String sheetId) {
        jdbc.query("SELECT period_id, person_id, amount FROM rollover_last_opening WHERE sheet_id = ?", rs -> {
            long periodId = rs.getLong("period_id");
            long personId = rs.getLong("person_id");
            jdbc.update("UPDATE period_opening SET amount = amount - ? WHERE sheet_id = ? AND period_id = ? AND person_id = ?",
                    rs.getBigDecimal("amount"), sheetId, periodId, personId);
            jdbc.update("DELETE FROM period_opening WHERE sheet_id = ? AND period_id = ? AND person_id = ? AND amount <= 0",
                    sheetId, periodId, personId);
        }, sheetId);
        for (Long periodId : lastRolloverPeriods(sheetId)) {
            jdbc.update("DELETE FROM period_pool_cut WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
            jdbc.update("DELETE FROM period_person_total WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
            jdbc.update("DELETE FROM period_opening WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
            jdbc.update("DELETE FROM period WHERE id = ? AND sheet_id = ?", periodId, sheetId);
        }
        forgetRollover(sheetId);
    }

    private void forgetRollover(String sheetId) {
        jdbc.update("DELETE FROM rollover_last_opening WHERE sheet_id = ?", sheetId);
        jdbc.update("DELETE FROM rollover_last_period WHERE sheet_id = ?", sheetId);
        jdbc.update("DELETE FROM rollover_last WHERE sheet_id = ?", sheetId);
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

    /** Fix each pool member's cut for a period, by pool name then member key (null for a member with no cut),
     *  replacing any fixed before. */
    public void fixPoolCuts(String sheetId, long periodId, Map<String, Map<String, String>> cuts) {
        jdbc.update("DELETE FROM period_pool_cut WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
        for (Map.Entry<String, Map<String, String>> pool : cuts.entrySet()) {
            for (Map.Entry<String, String> m : pool.getValue().entrySet()) {
                jdbc.update("INSERT INTO period_pool_cut (period_id, sheet_id, pool_name, member_key, cut) VALUES (?, ?, ?, ?, ?)",
                        periodId, sheetId, pool.getKey(), m.getKey(), m.getValue() == null ? null : new BigDecimal(m.getValue()));
            }
        }
    }

    /** Each closed or soft-closed period's fixed pool cuts, by period id, then pool name, then member key. */
    public Map<Long, Map<String, Map<String, String>>> poolCuts(String sheetId) {
        Map<Long, Map<String, Map<String, String>>> cuts = new HashMap<>();
        jdbc.query("SELECT period_id, pool_name, member_key, cut FROM period_pool_cut WHERE sheet_id = ? ORDER BY id", rs -> {
            BigDecimal cut = rs.getBigDecimal("cut");
            cuts.computeIfAbsent(rs.getLong("period_id"), k -> new HashMap<>())
                    .computeIfAbsent(rs.getString("pool_name"), k -> new HashMap<>())
                    .put(rs.getString("member_key"), cut == null ? null : cut.setScale(2).toPlainString());
        }, sheetId);
        return cuts;
    }

    /** Reopen a closed or soft-closed period, dropping its fixed due-total and figures. */
    public void reopen(String sheetId, long periodId) {
        jdbc.update("DELETE FROM period_pool_cut WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
        jdbc.update("DELETE FROM period_person_total WHERE period_id = ? AND sheet_id = ?", periodId, sheetId);
        jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL WHERE id = ? AND sheet_id = ?", periodId,
                sheetId);
    }

    /** Lock or unlock a period. */
    public void setLocked(String sheetId, long periodId, boolean locked) {
        jdbc.update("UPDATE period SET locked = ? WHERE id = ? AND sheet_id = ?", locked, periodId, sheetId);
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
        return new Period(keys.getKey().longValue(), name, from.toString(), to.toString(), false, null, Map.of(), Map.of(), false, cap, Map.of(), false);
    }
}
