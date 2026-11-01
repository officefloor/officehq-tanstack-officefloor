package net.officefloor.hq.app.period;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The periods kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("name"),
            rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString());

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every period on the sheet, by from-date. */
    public List<Period> list(String sheet) {
        return jdbc.query("SELECT name, from_date, to_date FROM period WHERE sheet = ? ORDER BY from_date", ROW,
                sheet);
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
}
