package net.officefloor.hq.app.rate;

import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The rates kept per sheet: per grade, one for each from-date (and at most one with none). A sheet is given its
 * parents' rates too, up to {@link Rate#LEVELS} up, each at its level.
 */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("grade"),
            rs.getBigDecimal("pct").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString(),
            rs.getDate("to_date") == null ? null : rs.getDate("to_date").toLocalDate().toString());

    private final JdbcTemplate jdbc;

    private final Settings settings;

    public Rates(JdbcTemplate jdbc, Settings settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    /** Every rate on the sheet, its own by grade then from-date (the one with none first), then its parents' by level. */
    public List<Rate> list(String sheet) {
        List<Rate> all = new ArrayList<>(own(sheet));
        String parent = settings.parent(sheet);
        for (int level = 1; level <= Rate.LEVELS && !parent.isEmpty(); level++) {
            for (Rate rate : own(parent)) {
                all.add(rate.up(level));
            }
            parent = settings.parent(parent);
        }
        return all;
    }

    /** The rates kept on the sheet itself, by grade then from-date (the one with none first). */
    public List<Rate> own(String sheet) {
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
