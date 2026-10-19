package net.officefloor.hq.app.period;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods of each sheet, listed by from-date. */
@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("sheet_id"), rs.getString("name"),
            rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Period> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date, id", ROW, sheetId);
    }

    public Period findByName(String sheetId, String name) {
        List<Period> found = jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name = ?", ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The closed period the date falls in, if any. */
    public Optional<Period> closedOn(String sheetId, LocalDate date) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?",
                ROW, sheetId, Date.valueOf(date), Date.valueOf(date)).stream().findFirst();
    }

    public void create(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }

    public void saveClosed(Period period) {
        jdbc.update("UPDATE period SET closed = ?, closed_total = ? WHERE sheet_id = ? AND name = ?",
                period.closed(), period.closedTotal(), period.sheetId(), period.name());
    }
}
