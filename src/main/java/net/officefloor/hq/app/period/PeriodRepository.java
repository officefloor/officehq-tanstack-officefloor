package net.officefloor.hq.app.period;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods of each sheet, listed by from-date. */
@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("sheet_id"), rs.getString("name"),
            rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate());

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Period> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date, id", ROW, sheetId);
    }

    public void create(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()));
    }
}
