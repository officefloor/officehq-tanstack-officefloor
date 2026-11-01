package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("name"),
            rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString(),
            rs.getBoolean("closed"),
            rs.getBigDecimal("closed_total") == null ? null : rs.getBigDecimal("closed_total").toPlainString());

    private static final String COLUMNS = "SELECT name, from_date, to_date, closed, closed_total FROM period";

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<Period> list(String sheet) {
        return jdbc.query(COLUMNS + " WHERE sheet = ? ORDER BY from_date", ROW, sheet);
    }

    /** The sheet's period of the name, or null when there is none. */
    public Period find(String sheet, String name) {
        List<Period> found = jdbc.query(COLUMNS + " WHERE sheet = ? AND name = ?", ROW, sheet, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the date falls inside a closed period of the sheet. */
    public boolean closedOn(String sheet, LocalDate date) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet = ? AND closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheet, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Whether the sheet has a period of the name. */
    public boolean named(String sheet, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet = ? AND name = ?", Integer.class,
                sheet, name);
        return n != null && n > 0;
    }

    /** Whether any period on the sheet shares a date with the inclusive range. */
    public boolean overlaps(String sheet, PeriodInput input) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet = ? AND from_date <= ? AND to_date >= ?", Integer.class,
                sheet, Date.valueOf(input.to()), Date.valueOf(input.from()));
        return n != null && n > 0;
    }

    public void add(String sheet, PeriodInput input) {
        jdbc.update("INSERT INTO period (sheet, name, from_date, to_date) VALUES (?, ?, ?, ?)", sheet, input.name(),
                Date.valueOf(input.from()), Date.valueOf(input.to()));
    }

    /** Closes the period with its due-total fixed; false when it was already closed (or is gone). */
    public boolean close(String sheet, String name, BigDecimal total) {
        return jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet = ? AND name = ? AND NOT closed",
                total, sheet, name) == 1;
    }
}
