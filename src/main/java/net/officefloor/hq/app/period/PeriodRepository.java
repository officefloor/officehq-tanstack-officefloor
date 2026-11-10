package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods store: one row per period, kept per sheet. */
@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class));

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, name, from_date, to_date FROM period WHERE sheet_id = ?"
                + " ORDER BY from_date", ROW, sheetId);
    }

    public void insert(Period period) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date) VALUES (?, ?, ?, ?)",
                period.sheetId(), period.name(), period.from(), period.to());
    }
}
