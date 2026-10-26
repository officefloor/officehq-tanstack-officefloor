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

/** The periods of each sheet, none overlapping another on its sheet. */
@Repository
public class PeriodRepository {

    static final String NAME_TAKEN = "That period name is already in use on this sheet";

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getLong("id"), rs.getString("sheet_id"),
            rs.getString("name"), rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"), Map.of(), rs.getBoolean("soft_closed"),
            (Integer) rs.getObject("claim_cap"));

    private static final String COLUMNS = "id, sheet_id, name, from_date, to_date, closed, closed_total, soft_closed, claim_cap";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        List<Period> periods = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ?"
                + " ORDER BY from_date, id", ROW, sheetId);
        Map<Long, Map<Long, BigDecimal>> totals = new HashMap<>();
        jdbc.query("SELECT t.period_id, t.person_id, t.total FROM period_person_total t JOIN period p ON p.id = t.period_id"
                + " WHERE p.sheet_id = ?", rs -> {
                    totals.computeIfAbsent(rs.getLong("period_id"), k -> new LinkedHashMap<>())
                            .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                }, sheetId);
        return periods.stream().map(p -> withTotals(p, totals.getOrDefault(p.id(), Map.of()))).toList();
    }

    private static Period withTotals(Period p, Map<Long, BigDecimal> personTotals) {
        return new Period(p.id(), p.sheetId(), p.name(), p.from(), p.to(), p.closed(), p.closedTotal(), personTotals, p.soft(), p.cap());
    }

    /** Whether a period on the sheet has this name, ignoring case and surrounding spaces. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether a period on the sheet shares a day with the dates from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    public Period insert(String sheetId, String name, LocalDate from, LocalDate to) {
        return insert(sheetId, name, from, to, null);
    }

    public Period insert(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO period (sheet_id, name, from_date, to_date, claim_cap) VALUES (?, ?, ?, ?, ?)", new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            ps.setObject(5, cap, java.sql.Types.INTEGER);
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), sheetId, name, from, to, false, null, Map.of(), false, cap);
    }

    public Period find(String sheetId, long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND id = ?", ROW, sheetId, id)
                .stream().findFirst().orElse(null);
    }

    /** The period whose dates contain the date; null when none does. */
    public Period on(String sheetId, LocalDate date) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** Whether the date falls inside a closed period of the sheet. */
    public boolean closedOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /**
     * Close the period (open or soft-closed) with its due-total and each person's (by person id) fixed afresh. Whether
     * it was not yet closed: of two users closing it, one wins.
     */
    public boolean close(long id, BigDecimal total, Map<Long, BigDecimal> personTotals) {
        if (jdbc.update("UPDATE period SET closed = TRUE, soft_closed = FALSE, closed_total = ? WHERE id = ? AND NOT closed",
                total, id) != 1) {
            return false;
        }
        fixPersonTotals(id, personTotals);
        return true;
    }

    /**
     * Soft-close the open period with its due-total and each person's (by person id) fixed. Whether it was still open
     * (neither closed nor soft-closed): of two users soft-closing it, one wins.
     */
    public boolean softClose(long id, BigDecimal total, Map<Long, BigDecimal> personTotals) {
        if (jdbc.update("UPDATE period SET soft_closed = TRUE, closed_total = ? WHERE id = ? AND NOT closed AND NOT soft_closed",
                total, id) != 1) {
            return false;
        }
        fixPersonTotals(id, personTotals);
        return true;
    }

    private void fixPersonTotals(long id, Map<Long, BigDecimal> personTotals) {
        jdbc.update("DELETE FROM period_person_total WHERE period_id = ?", id);
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (period_id, person_id, total) VALUES (?, ?, ?)", id, personId,
                personTotal));
    }

    /** Whether the date falls inside a soft-closed period of the sheet. */
    public boolean softClosedOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND soft_closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Reopen the closed or soft-closed period, its due-totals live again. Whether it was still closed or soft-closed: of two users reopening it, one wins. */
    public boolean reopen(long id) {
        if (jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL WHERE id = ? AND (closed OR soft_closed)", id) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE period_id = ?", id);
        return true;
    }
}
