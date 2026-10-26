package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of periods, kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
    }

    /** The sheet's period of that name, or null when there is none. */
    public Period find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name = ?", ROW, sheetId, name).stream()
                .findFirst().orElse(null);
    }

    /** The sheet's period containing the date, or null when none does. */
    public Period on(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND from_date <= ? AND ? <= to_date",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    /** The sheet's closed period containing the date, or null when none does. */
    public Period closedOn(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND ? <= to_date",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }

    /** Closes the period at the total; false when it was already closed (by someone else). */
    public boolean close(Period period, BigDecimal total) {
        return jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                total, period.sheetId(), period.name()) == 1;
    }

    /** Reopens the period, dropping its fixed total; false when it was not closed (someone else reopened it). */
    public boolean reopen(Period period) {
        return jdbc.update("UPDATE period SET closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ? AND closed",
                period.sheetId(), period.name()) == 1;
    }
}
