package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The periods of each sheet. */
@Repository
public class Periods {

    private static final String COLUMNS = "SELECT sheet_id, name, from_date, to_date, closed, closed_total FROM period";

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

    /** The closed period of the sheet whose dates contain the date, or null when there is none. */
    public Period closedOn(String sheetId, LocalDate date) {
        return jdbc.query(COLUMNS + " WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?",
                Periods::period, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst().orElse(null);
    }

    public void add(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }

    /** Close the period, fixing its due-total. */
    public void close(Period period, BigDecimal total) {
        jdbc.update("UPDATE period SET closed = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ?",
                total, period.sheetId(), period.name());
    }

    private static Period period(ResultSet rs, int i) throws SQLException {
        return new Period(rs.getString("sheet_id"), rs.getString("name"), rs.getDate("from_date").toLocalDate(),
                rs.getDate("to_date").toLocalDate(), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));
    }
}
