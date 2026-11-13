package net.officefloor.hq.app.period;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final String COLUMNS = "sheet_id, name, from_date, to_date, closed, closed_total";

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, earliest first. */
    public List<Period> findAll(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date",
                PeriodRepository::map, sheetId);
    }

    /** The sheet's period with this name, or null. */
    public Period find(String sheetId, String name) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND name = ?",
                PeriodRepository::map, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet's closed period containing the date, or null. */
    public Period closedOn(String sheetId, LocalDate date) {
        return findAll(sheetId).stream().filter((p) -> p.closed() && p.contains(date)).findFirst().orElse(null);
    }

    public void insert(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), period.from(), period.to());
    }

    /** Store the period's closed flag and fixed due-total (null when reopened). */
    public void saveClosed(Period period) {
        jdbc.update("UPDATE period SET closed = ?, closed_total = ? WHERE sheet_id = ? AND name = ?",
                period.closed(), period.closedTotal(), period.sheetId(), period.name());
    }

    private static Period map(ResultSet rs, int n) throws SQLException {
        return new Period(rs.getString("sheet_id"), rs.getString("name"), rs.getObject("from_date", LocalDate.class),
                rs.getObject("to_date", LocalDate.class), rs.getBoolean("closed"), rs.getBigDecimal("closed_total"));
    }
}
