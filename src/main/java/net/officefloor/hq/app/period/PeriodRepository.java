package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods store: one row per period, kept per sheet. */
@Repository
public class PeriodRepository {

    private static final String COLUMNS = "sheet_id, name, from_date, to_date, closed, closed_total";

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));

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

    /** The closed period on the sheet whose dates contain the date, or null when none does. */
    public Period closedOn(String sheetId, LocalDate date) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND closed"
                + " AND from_date <= ? AND to_date >= ?", ROW, sheetId, date, date);
        return found.isEmpty() ? null : found.get(0);
    }

    public void insert(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), period.from(), period.to());
    }

    /**
     * Mark the period closed with its fixed due-total, when it is still open in the store; false
     * when another request closed it first.
     */
    public boolean markClosed(Period period) {
        return jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                period.closedTotal(), period.sheetId(), period.name()) == 1;
    }
}
