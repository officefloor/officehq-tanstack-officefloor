package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, n) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class).toString(),
            rs.getObject("to_date", LocalDate.class).toString());

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, name, from_date, to_date FROM period WHERE sheet_id = ? "
                + "ORDER BY from_date", ROW, sheetId);
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
        return new Period(sheetId, name, from.toString(), to.toString());
    }
}
