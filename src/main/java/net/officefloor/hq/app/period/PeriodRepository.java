package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.pool.PoolRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class).toString(),
            rs.getObject("to_date", LocalDate.class).toString(), rs.getBoolean("closed"),
            rs.getBigDecimal("closed_total") == null ? null
                    : rs.getBigDecimal("closed_total").setScale(2).toPlainString(), Map.of(),
            rs.getBoolean("soft_closed"), (Integer) rs.getObject("claim_cap", Integer.class), Map.of(), Map.of(),
            rs.getBoolean("locked"));

    private static final String COLUMNS = "SELECT sheet_id, name, from_date, to_date, closed, closed_total, soft_closed, claim_cap, locked FROM period ";

    private final JdbcTemplate jdbc;
    private final PoolRepository pools;

    public PeriodRepository(JdbcTemplate jdbc, PoolRepository pools) {
        this.jdbc = jdbc;
        this.pools = pools;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId).stream()
                .map(this::withPersonTotals).map(this::withOpenings).toList();
    }

    /** The period with each person's due-total and carried figure fixed when it was closed. */
    private Period withPersonTotals(Period p) {
        if (!p.fixed()) return p;
        Map<Long, String> totals = new LinkedHashMap<>();
        Map<Long, String> carried = new LinkedHashMap<>();
        jdbc.query("SELECT t.person_id, t.total, t.carried FROM period_person_total t JOIN period p ON p.id = t.period_id "
                + "WHERE p.sheet_id = ? AND p.name = ? ORDER BY t.person_id",
                rs -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total").setScale(2).toPlainString());
                    carried.put(rs.getLong("person_id"), rs.getBigDecimal("carried").setScale(2).toPlainString());
                },
                p.sheetId(), p.name());
        return new Period(p.sheetId(), p.name(), p.from(), p.to(), p.closed(), p.closedTotal(), totals, p.soft(), p.cap(),
                carried, p.personOpenings(), p.locked());
    }

    /** The period with each person's opening balance for it. */
    private Period withOpenings(Period p) {
        return new Period(p.sheetId(), p.name(), p.from(), p.to(), p.closed(), p.closedTotal(), p.personTotals(), p.soft(),
                p.cap(), p.personCarried(), openings(p.sheetId(), p.name()), p.locked());
    }

    /** Each person's opening balance for the sheet's period, by person id. */
    private Map<Long, String> openings(String sheetId, String name) {
        Map<Long, String> openings = new LinkedHashMap<>();
        jdbc.query("SELECT o.person_id, o.amount FROM period_opening o JOIN period p ON p.id = o.period_id "
                + "WHERE p.sheet_id = ? AND p.name = ? ORDER BY o.person_id",
                rs -> {
                    openings.put(rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2).toPlainString());
                },
                sheetId, name);
        return openings;
    }

    /** The sheet's period with the name, compared ignoring case; null when there is none. */
    public Period findByName(String sheetId, String name) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? AND name_key = LOWER(?)", ROW, sheetId, name)
                .stream().findFirst().map(this::withPersonTotals).map(this::withOpenings).orElse(null);
    }

    /** Add the amount to the person's opening balance for the sheet's period. */
    public void addOpening(String sheetId, String period, long personId, BigDecimal amount) {
        int n = jdbc.update("UPDATE period_opening SET amount = amount + ? WHERE person_id = ? AND period_id = "
                + "(SELECT id FROM period WHERE sheet_id = ? AND name = ?)", amount, personId, sheetId, period);
        if (n == 0) {
            jdbc.update("INSERT INTO period_opening (period_id, person_id, amount) SELECT id, ?, ? FROM period "
                    + "WHERE sheet_id = ? AND name = ?", personId, amount, sheetId, period);
        }
    }

    /** The sheet's period with the name; null when there is none. */
    public Period find(String sheetId, String name) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? AND name = ?", ROW, sheetId, name)
                .stream().findFirst().map(this::withPersonTotals).map(this::withOpenings).orElse(null);
    }

    /** Refuse a change to a claim whose due-by (yyyy-MM-dd) is inside one of the sheet's closed periods:
     *  a claim belongs to the period its due-by falls in. */
    public void requireOpen(String sheetId, String dueBy) throws PeriodClosedException {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId,
                LocalDate.parse(dueBy), LocalDate.parse(dueBy));
        if (n != null && n > 0) {
            throw new PeriodClosedException("The claim's due-by is inside a closed period");
        }
    }

    /** Whether the date (yyyy-MM-dd) falls inside one of the sheet's soft-closed periods. */
    public boolean softClosedOn(String sheetId, String date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND soft_closed "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId,
                LocalDate.parse(date), LocalDate.parse(date));
        return n != null && n > 0;
    }

    /** Close the period, fixing its due-total and each person's due-total and carried figure for it (afresh
     *  when it was soft-closed), and every pool member's cut for it as it now stands; soft: soft-close it
     *  instead, claims in it can still be added and changed. */
    public Period close(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried, String user, boolean soft) {
        jdbc.update("DELETE FROM period_person_total WHERE period_id IN "
                + "(SELECT id FROM period WHERE sheet_id = ? AND name = ?)", period.sheetId(), period.name());
        for (Map.Entry<Long, BigDecimal> e : personTotals.entrySet()) {
            jdbc.update("INSERT INTO period_person_total (period_id, person_id, total, carried) SELECT id, ?, ?, ? "
                    + "FROM period WHERE sheet_id = ? AND name = ?", e.getKey(), e.getValue(),
                    personCarried.getOrDefault(e.getKey(), BigDecimal.ZERO.setScale(2)), period.sheetId(), period.name());
        }
        pools.freeze(period.sheetId(), period.name());
        jdbc.update("UPDATE period SET closed = ?, soft_closed = ?, closed_total = ?, closed_by = ?, "
                + "closed_at = CURRENT_TIMESTAMP WHERE sheet_id = ? AND name = ?",
                !soft, soft, total, user, period.sheetId(), period.name());
        Map<Long, String> shown = new LinkedHashMap<>();
        personTotals.forEach((id, t) -> shown.put(id, t.toPlainString()));
        Map<Long, String> carried = new LinkedHashMap<>();
        personTotals.keySet().forEach((id) -> carried.put(id,
                personCarried.getOrDefault(id, BigDecimal.ZERO.setScale(2)).toPlainString()));
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), !soft,
                total.toPlainString(), shown, soft, period.cap(), carried, openings(period.sheetId(), period.name()),
                period.locked());
    }

    /** Reopen the closed or soft-closed period: its due-total and pool cuts are live again. */
    public Period reopen(Period period) {
        pools.thaw(period.sheetId(), period.name());
        jdbc.update("DELETE FROM period_person_total WHERE period_id IN "
                + "(SELECT id FROM period WHERE sheet_id = ? AND name = ?)", period.sheetId(), period.name());
        jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL, closed_by = NULL, "
                + "closed_at = NULL WHERE sheet_id = ? AND name = ?", period.sheetId(), period.name());
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), false, null, Map.of(), false, period.cap(), Map.of(),
                openings(period.sheetId(), period.name()), period.locked());
    }

    /** Lock or unlock the period. */
    public Period lock(Period period, boolean locked) {
        jdbc.update("UPDATE period SET locked = ? WHERE sheet_id = ? AND name = ?", locked, period.sheetId(), period.name());
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), period.closed(), period.closedTotal(),
                period.personTotals(), period.soft(), period.cap(), period.personCarried(), period.personOpenings(), locked);
    }

    /** Whether another period on the sheet has the name, compared without regard to case. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name_key = LOWER(?)",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether a period on the sheet shares a day with from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, to, from);
        return n != null && n > 0;
    }

    public Period add(String sheetId, String name, LocalDate from, LocalDate to, String user) {
        return add(sheetId, name, from, to, user, null);
    }

    /** Add a period; cap: the most OPEN or PENDING claims a person may have in it, null for none. */
    public Period add(String sheetId, String name, LocalDate from, LocalDate to, String user, Integer cap) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, created_by, claim_cap) "
                + "VALUES (?, ?, ?, ?, ?, ?)", sheetId, name, from, to, user, cap);
        return new Period(sheetId, name, from.toString(), to.toString(), false, null, Map.of(), false, cap, Map.of(), Map.of(), false);
    }

    /** The sheet's period whose dates contain the date (yyyy-MM-dd); null when there is none. */
    public Period containing(String sheetId, String date) {
        LocalDate d = LocalDate.parse(date);
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?", ROW, sheetId, d, d)
                .stream().findFirst().orElse(null);
    }

    /** Move the person {@code fromId}'s fixed due-totals and carried figures of closed periods, and their
     *  opening balances, to {@code intoId}, added to into's where into has one. */
    public void moveTotals(String sheetId, long fromId, long intoId) {
        String ofSheet = "period_id IN (SELECT id FROM period WHERE sheet_id = ?)";
        jdbc.update("UPDATE period_person_total t SET total = total + (SELECT f.total FROM period_person_total f "
                + "WHERE f.period_id = t.period_id AND f.person_id = ?), carried = carried + (SELECT f.carried "
                + "FROM period_person_total f WHERE f.period_id = t.period_id AND f.person_id = ?) WHERE t.person_id = ? AND t." + ofSheet
                + " AND t.period_id IN (SELECT f.period_id FROM period_person_total f WHERE f.person_id = ?)",
                fromId, fromId, intoId, sheetId, fromId);
        jdbc.update("DELETE FROM period_person_total WHERE person_id = ? AND " + ofSheet + " AND period_id IN "
                + "(SELECT t.period_id FROM period_person_total t WHERE t.person_id = ?)", fromId, sheetId, intoId);
        jdbc.update("UPDATE period_person_total SET person_id = ? WHERE person_id = ? AND " + ofSheet,
                intoId, fromId, sheetId);
        jdbc.update("UPDATE period_opening t SET amount = amount + (SELECT f.amount FROM period_opening f "
                + "WHERE f.period_id = t.period_id AND f.person_id = ?) WHERE t.person_id = ? AND t." + ofSheet
                + " AND t.period_id IN (SELECT f.period_id FROM period_opening f WHERE f.person_id = ?)",
                fromId, intoId, sheetId, fromId);
        jdbc.update("DELETE FROM period_opening WHERE person_id = ? AND " + ofSheet + " AND period_id IN "
                + "(SELECT t.period_id FROM period_opening t WHERE t.person_id = ?)", fromId, sheetId, intoId);
        jdbc.update("UPDATE period_opening SET person_id = ? WHERE person_id = ? AND " + ofSheet,
                intoId, fromId, sheetId);
    }
}
