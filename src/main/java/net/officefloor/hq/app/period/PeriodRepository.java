package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            rs.getBoolean("soft_closed"));

    private static final String COLUMNS = "SELECT sheet_id, name, from_date, to_date, closed, closed_total, soft_closed FROM period ";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId).stream()
                .map(this::withPersonTotals).toList();
    }

    /** The period with each person's due-total fixed when it was closed. */
    private Period withPersonTotals(Period p) {
        if (!p.fixed()) return p;
        Map<Long, String> totals = new LinkedHashMap<>();
        jdbc.query("SELECT t.person_id, t.total FROM period_person_total t JOIN period p ON p.id = t.period_id "
                + "WHERE p.sheet_id = ? AND p.name = ? ORDER BY t.person_id",
                rs -> { totals.put(rs.getLong("person_id"), rs.getBigDecimal("total").setScale(2).toPlainString()); },
                p.sheetId(), p.name());
        return new Period(p.sheetId(), p.name(), p.from(), p.to(), p.closed(), p.closedTotal(), totals, p.soft());
    }

    /** The sheet's period with the name; null when there is none. */
    public Period find(String sheetId, String name) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? AND name = ?", ROW, sheetId, name)
                .stream().findFirst().map(this::withPersonTotals).orElse(null);
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

    /** Close the period, fixing its due-total and each person's due-total for it (afresh when it was
     *  soft-closed); soft: soft-close it instead, claims in it can still be added and changed. */
    public Period close(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals, String user,
            boolean soft) {
        jdbc.update("DELETE FROM period_person_total WHERE period_id IN "
                + "(SELECT id FROM period WHERE sheet_id = ? AND name = ?)", period.sheetId(), period.name());
        for (Map.Entry<Long, BigDecimal> e : personTotals.entrySet()) {
            jdbc.update("INSERT INTO period_person_total (period_id, person_id, total) SELECT id, ?, ? "
                    + "FROM period WHERE sheet_id = ? AND name = ?",
                    e.getKey(), e.getValue(), period.sheetId(), period.name());
        }
        jdbc.update("UPDATE period SET closed = ?, soft_closed = ?, closed_total = ?, closed_by = ?, "
                + "closed_at = CURRENT_TIMESTAMP WHERE sheet_id = ? AND name = ?",
                !soft, soft, total, user, period.sheetId(), period.name());
        Map<Long, String> shown = new LinkedHashMap<>();
        personTotals.forEach((id, t) -> shown.put(id, t.toPlainString()));
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), !soft,
                total.toPlainString(), shown, soft);
    }

    /** Reopen the closed or soft-closed period: its due-total is live again. */
    public Period reopen(Period period) {
        jdbc.update("DELETE FROM period_person_total WHERE period_id IN "
                + "(SELECT id FROM period WHERE sheet_id = ? AND name = ?)", period.sheetId(), period.name());
        jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL, closed_by = NULL, "
                + "closed_at = NULL WHERE sheet_id = ? AND name = ?", period.sheetId(), period.name());
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), false, null, Map.of(), false);
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
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, created_by) VALUES (?, ?, ?, ?, ?)",
                sheetId, name, from, to, user);
        return new Period(sheetId, name, from.toString(), to.toString(), false, null, Map.of(), false);
    }
}
