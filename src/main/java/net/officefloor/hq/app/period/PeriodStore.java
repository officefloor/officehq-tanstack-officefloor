package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods on each sheet. */
@Repository
public class PeriodStore {

    private final RowMapper<PeriodResponse> ROW = (rs, i) -> PeriodResponse.of(rs.getString("name"),
            rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"),
            rs.getBoolean("closed") || rs.getBoolean("soft") ? personTotals(rs.getString("sheet_id"), rs.getString("name"))
                    : Map.of(),
            rs.getBoolean("soft"), cap(rs),
            rs.getBoolean("closed") || rs.getBoolean("soft") ? personCarried(rs.getString("sheet_id"), rs.getString("name"),
                    "period_person_total") : Map.of());

    private final JdbcTemplate jdbc;

    public PeriodStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<PeriodResponse> list(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
    }

    /** Every period on the sheet as the periods and their fixed figures stood at the end of the date (see
     *  {@link History}), by from-date. */
    public List<PeriodResponse> listAsOf(String sheetId, LocalDate date) {
        String totals = History.asOf("period_person_total", "sheet_id, period_name, person_id", date);
        return jdbc.query("SELECT * FROM " + History.asOf("period", "id", date) + " WHERE sheet_id = ? ORDER BY from_date",
                (rs, i) -> PeriodResponse.of(rs.getString("name"), rs.getObject("from_date", LocalDate.class),
                        rs.getObject("to_date", LocalDate.class), rs.getBoolean("closed"),
                        rs.getBigDecimal("closed_total"),
                        rs.getBoolean("closed") || rs.getBoolean("soft")
                                ? personTotals(rs.getString("sheet_id"), rs.getString("name"), totals)
                                : Map.of(),
                        rs.getBoolean("soft"), cap(rs),
                        rs.getBoolean("closed") || rs.getBoolean("soft")
                                ? personCarried(rs.getString("sheet_id"), rs.getString("name"), totals)
                                : Map.of()),
                sheetId);
    }

    /** The period on the sheet with this name, or null when there is none. */
    public PeriodResponse find(String sheetId, String name) {
        List<PeriodResponse> found = jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name = ?", ROW, sheetId,
                name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The closed period on the sheet whose dates contain the date, or null when none does. */
    public PeriodResponse closedOn(String sheetId, LocalDate date) {
        List<PeriodResponse> found = jdbc.query(
                "SELECT * FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?", ROW, sheetId,
                date, date);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the period on the sheet whose dates contain the date is soft-closed. */
    public boolean softOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND soft AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, date, date);
        return n != null && n > 0;
    }

    /** The period on the sheet whose dates contain the date, or null when none does. */
    public PeriodResponse on(String sheetId, LocalDate date) {
        List<PeriodResponse> found = jdbc.query(
                "SELECT * FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?", ROW, sheetId, date, date);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a period on the sheet has this name, ignoring case. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                Integer.class, sheetId, name);
        return n != null && n > 0;
    }

    /** Whether a period on the sheet shares a day with from..to (inclusive). */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?", Integer.class,
                sheetId, to, from);
        return n != null && n > 0;
    }

    public void add(String sheetId, PeriodInput period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, open_cap) VALUES (?, ?, ?, ?, ?)", sheetId,
                period.name(), period.from(), period.to(), period.cap());
    }

    private static Integer cap(java.sql.ResultSet rs) throws java.sql.SQLException {
        int cap = rs.getInt("open_cap");
        return rs.wasNull() ? null : cap;
    }

    /** Each person's due-total fixed when the period was closed, by person id. */
    private Map<Long, BigDecimal> personTotals(String sheetId, String name) {
        return personTotals(sheetId, name, "period_person_total");
    }

    private Map<Long, BigDecimal> personTotals(String sheetId, String name, String table) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, total FROM " + table + " WHERE sheet_id = ? AND period_name = ? ORDER BY person_id",
                rs -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total"));
                }, sheetId, name);
        return totals;
    }

    /** Each person's carried figure fixed with their due-total when the period was closed, by person id. */
    private Map<Long, BigDecimal> personCarried(String sheetId, String name, String table) {
        Map<Long, BigDecimal> carried = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, carried FROM " + table + " WHERE sheet_id = ? AND period_name = ? ORDER BY person_id",
                rs -> {
                    carried.put(rs.getLong("person_id"), rs.getBigDecimal("carried"));
                }, sheetId, name);
        return carried;
    }

    /** Each person's opening balance for the period, by person id (a person missing has none). */
    public Map<Long, BigDecimal> openings(String sheetId, String name) {
        Map<Long, BigDecimal> amounts = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, amount FROM period_person_opening WHERE sheet_id = ? AND period_name = ?",
                rs -> {
                    amounts.put(rs.getLong("person_id"), rs.getBigDecimal("amount"));
                }, sheetId, name);
        return amounts;
    }

    /** Fixes each person's due-total and carried figure in the period as they are when it is closed. */
    public void fixPersonTotals(String sheetId, String name, Map<Long, BigDecimal> totals, Map<Long, BigDecimal> carried) {
        totals.forEach((personId, total) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                sheetId, name, personId, total, carried.getOrDefault(personId, BigDecimal.ZERO.setScale(2))));
    }

    /** Drops each person's due-total fixed in the period (as a soft close fixed them, before they are fixed afresh). */
    public void dropPersonTotals(String sheetId, String name) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
    }

    /** Closes the open (or soft-closed) period with its due-total fixed; false when it is already closed
     *  (another user closed it first). */
    public boolean close(String sheetId, String name, BigDecimal total) {
        return jdbc.update("UPDATE period SET closed = TRUE, soft = FALSE, closed_total = ? "
                + "WHERE sheet_id = ? AND name = ? AND NOT closed", total, sheetId, name) == 1;
    }

    /** Soft-closes the open period with its due-total fixed; false when it is already closed or soft-closed. */
    public boolean softClose(String sheetId, String name, BigDecimal total) {
        return jdbc.update("UPDATE period SET soft = TRUE, closed_total = ? "
                + "WHERE sheet_id = ? AND name = ? AND NOT closed AND NOT soft", total, sheetId, name) == 1;
    }

    /** Reopens the closed or soft-closed period, dropping its fixed due-total; false when it is neither
     *  (another user reopened it first). */
    public boolean reopen(String sheetId, String name) {
        dropPersonTotals(sheetId, name);
        return jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, closed_total = NULL "
                + "WHERE sheet_id = ? AND name = ? AND (closed OR soft)", sheetId, name) == 1;
    }
}
