package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class).toString(),
            rs.getObject("to_date", LocalDate.class).toString(), rs.getBoolean("closed"),
            rs.getBigDecimal("closed_total") == null ? null
                    : rs.getBigDecimal("closed_total").setScale(2).toPlainString());

    private static final String COLUMNS = "SELECT sheet_id, name, from_date, to_date, closed, closed_total FROM period ";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
    }

    /** The sheet's period with the name; null when there is none. */
    public Period find(String sheetId, String name) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? AND name = ?", ROW, sheetId, name)
                .stream().findFirst().orElse(null);
    }

    /** Refuse a change to a claim dated (yyyy-MM-dd) inside one of the sheet's closed periods. */
    public void requireOpen(String sheetId, String date) throws PeriodClosedException {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId,
                LocalDate.parse(date), LocalDate.parse(date));
        if (n != null && n > 0) {
            throw new PeriodClosedException("The claim's date is inside a closed period");
        }
    }

    /** Close the period, fixing its due-total. */
    public Period close(Period period, BigDecimal total, String user) {
        jdbc.update("UPDATE period SET closed = TRUE, closed_total = ?, closed_by = ?, "
                + "closed_at = CURRENT_TIMESTAMP WHERE sheet_id = ? AND name = ?",
                total, user, period.sheetId(), period.name());
        return new Period(period.sheetId(), period.name(), period.from(), period.to(), true,
                total.toPlainString());
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
        return new Period(sheetId, name, from.toString(), to.toString(), false, null);
    }
}
