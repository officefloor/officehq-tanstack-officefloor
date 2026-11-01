package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The periods of each sheet. */
@Repository
public class Periods {

    private static final String COLUMNS = "SELECT sheet_id, name, from_date, to_date, closed, closed_total, soft, cap, locked FROM period";

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<Period> onSheet(String sheetId) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? ORDER BY from_date", Periods::period, sheetId);
    }

    /** The period of this name on this sheet, or null when there is none. Locks its row for the
     *  rest of the transaction. */
    public Period find(String sheetId, String name) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND name = ? FOR UPDATE", Periods::period, sheetId, name)
                .stream().findFirst().orElse(null);
    }

    /** The (not soft-) closed period of the sheet whose dates contain the date, or null when there is none. */
    public Period closedOn(String sheetId, LocalDate date) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND closed AND NOT soft AND from_date <= ? AND to_date >= ?",
                Periods::period, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** The soft-closed period of the sheet whose dates contain the date, or null when there is none. */
    public Period softClosedOn(String sheetId, LocalDate date) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND closed AND soft AND from_date <= ? AND to_date >= ?",
                Periods::period, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** The capped period of the sheet whose dates contain the date, or null when there is none. */
    public Period cappedOn(String sheetId, LocalDate date) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND cap IS NOT NULL AND from_date <= ? AND to_date >= ?",
                Periods::period, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** How many of the person's claims due by in the period are OPEN or PENDING. */
    public int openIn(Period period, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND status IN ('OPEN', 'PENDING') AND due_by >= ? AND due_by <= ?", Integer.class,
                period.sheetId(), personId, Date.valueOf(period.from()), Date.valueOf(period.to()));
        return n == null ? 0 : n;
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, cap) VALUES (?, ?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()), period.cap());
    }

    /** Close (or, when `soft`, soft-close) the period, fixing its due-total and each person's (by
     *  person id) afresh, and each person's carried figure for it. */
    public void close(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried, boolean soft) {
        jdbc.update("UPDATE period SET closed = TRUE, soft = ?, closed_total = ? WHERE sheet_id = ? AND name = ?",
                soft, total, period.sheetId(), period.name());
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                period.sheetId(), period.name(), personId, personTotal,
                personCarried.getOrDefault(personId, BigDecimal.ZERO.setScale(2))));
    }

    /** Each person's due-total fixed when the period was closed, by person id; empty while it is open. */
    public Map<Long, BigDecimal> personTotals(Period period) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, total FROM period_person_total WHERE sheet_id = ? AND period_name = ?"
                + " ORDER BY person_id", rs -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                }, period.sheetId(), period.name());
        return totals;
    }

    /** Each person's carried figure fixed when the period was closed, by person id; empty while it is open. */
    public Map<Long, BigDecimal> personCarried(Period period) {
        Map<Long, BigDecimal> carried = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, carried FROM period_person_total WHERE sheet_id = ? AND period_name = ?"
                + " ORDER BY person_id", rs -> {
                    carried.put(rs.getLong("person_id"), rs.getBigDecimal("carried"));
                }, period.sheetId(), period.name());
        return carried;
    }

    /** Each person's opening balance for the period, by person id; a person with none is absent. */
    public Map<Long, BigDecimal> openings(Period period) {
        Map<Long, BigDecimal> openings = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, amount FROM period_opening WHERE sheet_id = ? AND period_name = ?"
                + " ORDER BY person_id", rs -> {
                    openings.put(rs.getLong("person_id"), rs.getBigDecimal("amount"));
                }, period.sheetId(), period.name());
        return openings;
    }

    /** Add the amount to the person's opening balance for the period. */
    public void addOpening(Period period, long personId, BigDecimal amount) {
        jdbc.update("MERGE INTO period_opening (sheet_id, period_name, person_id, amount) KEY (sheet_id, period_name, person_id) "
                + "VALUES (?, ?, ?, COALESCE((SELECT amount FROM period_opening WHERE sheet_id = ? AND period_name = ? "
                + "AND person_id = ?), 0) + ?)", period.sheetId(), period.name(), personId,
                period.sheetId(), period.name(), personId, amount);
    }

    /** Take the amount back off the person's opening balance for the period (a balance left at zero goes). */
    public void takeOpening(String sheetId, String periodName, long personId, BigDecimal amount) {
        jdbc.update("UPDATE period_opening SET amount = amount - ? WHERE sheet_id = ? AND period_name = ? AND person_id = ?",
                amount, sheetId, periodName, personId);
        jdbc.update("DELETE FROM period_opening WHERE sheet_id = ? AND period_name = ? AND person_id = ? AND amount = 0",
                sheetId, periodName, personId);
    }

    /** Remove the period, with every figure kept for it. */
    public void remove(String sheetId, String name) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        jdbc.update("DELETE FROM period_opening WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        jdbc.update("DELETE FROM period WHERE sheet_id = ? AND name = ?", sheetId, name);
    }

    /** Reopen the period: its due-total, each person's due-total and carried figure, and each pool
     *  member's cut for it, is live again. */
    public void reopen(Period period) {
        jdbc.update("DELETE FROM period_pool_cut WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
        jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ?",
                period.sheetId(), period.name());
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
    }

    /** Lock or unlock the period. */
    public void setLocked(Period period, boolean locked) {
        jdbc.update("UPDATE period SET locked = ? WHERE sheet_id = ? AND name = ?",
                locked, period.sheetId(), period.name());
    }

    private static Period period(ResultSet rs, int i) throws SQLException {
        return new Period(rs.getString("sheet_id"), rs.getString("name"), rs.getDate("from_date").toLocalDate(),
                rs.getDate("to_date").toLocalDate(), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"),
                rs.getBoolean("soft"), rs.getObject("cap", Integer.class), rs.getBoolean("locked"));
    }
}
