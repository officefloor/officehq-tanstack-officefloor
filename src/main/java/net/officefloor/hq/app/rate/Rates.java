package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rates for each grade of each sheet; a grade has at most one per from-date (or none). */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every rate on the sheet, by grade, then by from-date (the one with none first). */
    public List<Rate> onSheet(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date FROM rate WHERE sheet_id = ? ORDER BY grade, from_key",
                (rs, i) -> {
                    Date from = rs.getDate("from_date");
                    Date to = rs.getDate("to_date");
                    return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("pct"),
                            from != null ? from.toLocalDate() : null, to != null ? to.toLocalDate() : null);
                },
                sheetId);
    }

    /** The grade's rate in force on `date`: the one with the latest from-date on or before it, or
     *  null when there is none or that one's to-date is before `date`. */
    public BigDecimal pct(String sheetId, String grade, LocalDate date) {
        return jdbc.query("SELECT pct, to_date FROM rate WHERE sheet_id = ? AND grade = ? AND from_key <= ?"
                + " ORDER BY from_key DESC LIMIT 1",
                (rs, i) -> {
                    Date to = rs.getDate("to_date");
                    return to != null && to.toLocalDate().isBefore(date) ? null : rs.getBigDecimal("pct");
                }, sheetId, grade, Date.valueOf(date))
                .stream().filter(p -> p != null).findFirst().orElse(null);
    }

    /** Set the grade's rate from its from-date, replacing the one it had for that from-date. */
    public void save(Rate rate) {
        Date from = rate.from() != null ? Date.valueOf(rate.from()) : null;
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct(), from, rate.to() != null ? Date.valueOf(rate.to()) : null);
    }
}
