package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's rates, by grade then from-date (an undated rate first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT grade, pct, from_date FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST",
                (rs, i) -> new Rate(rs.getString("grade"), pct(rs.getBigDecimal("pct")),
                        from(rs.getObject("from_date", LocalDate.class))), sheetId);
    }

    /** Set a grade's rate from a from-date (null: from the beginning of time), replacing the one the
     *  grade had from that date. */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from) {
        Date on = from == null ? null : Date.valueOf(from);
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, on);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date) VALUES (?, ?, ?, ?)",
                sheetId, grade, pct, on);
        return new Rate(grade, pct(pct), from(from));
    }

    /** A from-date as shown (yyyy-mm-dd), or null when there is none. */
    static String from(LocalDate from) {
        return from == null ? null : from.toString();
    }

    /** A percent as shown: two decimals, a dot, no grouping. */
    static String pct(BigDecimal pct) {
        return pct.setScale(2).toPlainString();
    }
}
