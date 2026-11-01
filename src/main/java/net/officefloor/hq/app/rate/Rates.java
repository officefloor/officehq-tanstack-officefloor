package net.officefloor.hq.app.rate;

import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rates kept per sheet: per grade, one for each from-date (and at most one with none). */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("grade"),
            rs.getBigDecimal("pct").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString(),
            rs.getDate("to_date") == null ? null : rs.getDate("to_date").toLocalDate().toString());

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every rate on the sheet, by grade then from-date (the one with none first). */
    public List<Rate> list(String sheet) {
        return jdbc.query("SELECT grade, pct, from_date, to_date FROM rate WHERE sheet = ? ORDER BY grade, from_key", ROW,
                sheet);
    }

    /** Sets the grade's rate from the from-date (through its to-date), replacing the one already there for that grade and from-date. */
    public void set(String sheet, RateInput input) {
        Date from = input.from() == null ? null : Date.valueOf(input.from());
        Date to = input.to() == null ? null : Date.valueOf(input.to());
        if (jdbc.update(
                "UPDATE rate SET pct = ?, to_date = ? WHERE sheet = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                input.pct(), to, sheet, input.grade(), from) == 0) {
            jdbc.update("INSERT INTO rate (sheet, grade, pct, from_date, to_date) VALUES (?, ?, ?, ?, ?)", sheet,
                    input.grade(), input.pct(), from, to);
        }
    }
}
