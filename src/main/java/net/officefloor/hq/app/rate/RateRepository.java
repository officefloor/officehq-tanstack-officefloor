package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private static final String COLUMNS = "SELECT sheet_id, grade, pct, from_date, to_date FROM rate ";

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        LocalDate from = rs.getObject("from_date", LocalDate.class);
        LocalDate to = rs.getObject("to_date", LocalDate.class);
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"),
                rs.getBigDecimal("pct").setScale(2).toPlainString(), from == null ? null : from.toString(),
                to == null ? null : to.toString());
    };

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade, then by from-date (none first). A grade without a rate is not listed. */
    public List<Rate> list(String sheetId) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? ORDER BY grade, from_key", ROW, sheetId);
    }

    /** The grade's rate on the sheet in force on the date: the one with the latest from-date on or
     *  before it; null when there is none, or that one ended (its to-date) before the date. */
    public Rate find(String sheetId, String grade, LocalDate on) {
        return jdbc.query(COLUMNS + "WHERE sheet_id = ? AND grade = ? AND from_key <= ? "
                + "ORDER BY from_key DESC LIMIT 1", ROW, sheetId, grade, on).stream().findFirst()
                .filter((r) -> !r.endedBefore(on.toString())).orElse(null);
    }

    /** The sheet's rates as they stood at the end of the date: each as the last rate set while the
     *  sheet's today was on or before the date left it, by grade, then by from-date (none first). */
    public List<Rate> listAsOf(String sheetId, LocalDate asOf) {
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date FROM rate_history h WHERE h.sheet_id = ? "
                + "AND h.id = (SELECT MAX(h2.id) FROM rate_history h2 WHERE h2.sheet_id = h.sheet_id AND h2.grade = h.grade "
                + "AND h2.from_key = h.from_key AND (h2.on_date IS NULL OR h2.on_date <= ?)) ORDER BY grade, from_key",
                ROW, sheetId, asOf);
    }

    /** The grade's rate among {@code rates} in force on the date (yyyy-MM-dd), as {@link #find} finds it. */
    public static Rate inForce(List<Rate> rates, String grade, String date) {
        Rate found = null;
        for (Rate r : rates) {
            if (!r.grade().equals(grade) || (r.from() != null && r.from().compareTo(date) > 0)) continue;
            if (found == null || (found.from() == null ? "" : found.from()).compareTo(r.from() == null ? "" : r.from()) < 0) found = r;
        }
        return found == null || found.endedBefore(date) ? null : found;
    }

    /** Set (or replace) the grade's rate from the from-date (null: from the beginning of time) through
     *  the to-date (null: no end). */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, String user) {
        if (from == null) {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NULL", sheetId, grade);
        } else {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date = ?", sheetId, grade, from);
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, grade, pct, from, to, user);
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, on_date) "
                + "VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, grade, pct, from, to, sheetId);
        return new Rate(sheetId, grade, pct.toPlainString(), from == null ? null : from.toString(),
                to == null ? null : to.toString());
    }
}
