package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods on each sheet. */
@Repository
public class PeriodStore {

    private static final RowMapper<PeriodResponse> ROW = (rs, i) -> PeriodResponse.of(rs.getString("name"),
            rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));

    private final JdbcTemplate jdbc;

    public PeriodStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<PeriodResponse> list(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
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
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)", sheetId,
                period.name(), period.from(), period.to());
    }

    /** Closes the open period with its due-total fixed; false when it is already closed (another user
     *  closed it first). */
    public boolean close(String sheetId, String name, BigDecimal total) {
        return jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                total, sheetId, name) == 1;
    }
}
