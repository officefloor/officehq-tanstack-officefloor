package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods store: one row per period, kept per sheet. */
@Repository
public class PeriodRepository {

    private static final String COLUMNS = "sheet_id, name, from_date, to_date, closed, closed_total, soft";

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"),
            rs.getBoolean("soft"));

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ?"
                + " ORDER BY from_date", ROW, sheetId);
    }

    /** The period with this name on the sheet, or null when there is none. */
    public Period find(String sheetId, String name) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND name = ?",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The closed period on the sheet whose dates contain the date, or null when none does. A
     * soft-closed period does not count: its claims can still change.
     */
    public Period closedOn(String sheetId, LocalDate date) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND closed AND NOT soft"
                + " AND from_date <= ? AND to_date >= ?", ROW, sheetId, date, date);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The soft-closed period on the sheet whose dates contain the date, or null when none does. */
    public Period softClosedOn(String sheetId, LocalDate date) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND closed AND soft"
                + " AND from_date <= ? AND to_date >= ?", ROW, sheetId, date, date);
        return found.isEmpty() ? null : found.get(0);
    }

    public void insert(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), period.from(), period.to());
    }

    /**
     * Mark the period closed (or soft-closed) with its fixed due-total, when the store still holds it
     * as it was read (open, or soft-closed when {@code fromSoft}); false when another request changed
     * it first.
     */
    public boolean markClosed(Period period, boolean fromSoft) {
        return jdbc.update("UPDATE period SET closed = TRUE, soft = ?, closed_total = ? WHERE sheet_id = ? AND name = ?"
                + " AND closed = ? AND soft = ?",
                period.soft(), period.closedTotal(), period.sheetId(), period.name(), fromSoft, fromSoft) == 1;
    }

    /**
     * Reopen the period, clearing its fixed due-total: false when it is not closed, as when another
     * request reopened it first.
     */
    public boolean markReopened(Period period) {
        return jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ? AND closed",
                period.sheetId(), period.name()) == 1;
    }

    /** Fix each person's due-total for the closed period. */
    public void savePersonTotals(Period period, Map<Long, BigDecimal> totals) {
        for (Map.Entry<Long, BigDecimal> e : totals.entrySet()) {
            jdbc.update("INSERT INTO period_person_total (sheet_id, period_name, person_id, total) VALUES (?, ?, ?, ?)",
                    period.sheetId(), period.name(), e.getKey(), e.getValue());
        }
    }

    /** Clear the people's fixed due-totals for the reopened period. */
    public void deletePersonTotals(Period period) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?",
                period.sheetId(), period.name());
    }

    /** The people's fixed due-totals for the sheet's closed periods: by period name, then person id. */
    public Map<String, Map<Long, BigDecimal>> personTotals(String sheetId) {
        Map<String, Map<Long, BigDecimal>> found = new HashMap<>();
        jdbc.query("SELECT period_name, person_id, total FROM period_person_total WHERE sheet_id = ? ORDER BY person_id",
                rs -> {
                    found.computeIfAbsent(rs.getString("period_name"), n -> new LinkedHashMap<>())
                            .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                }, sheetId);
        return found;
    }

    /** The people's fixed due-totals for the period, by person id (empty while it is open). */
    public Map<Long, BigDecimal> personTotals(Period period) {
        return personTotals(period.sheetId()).getOrDefault(period.name(), Map.of());
    }
}
