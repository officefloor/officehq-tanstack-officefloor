package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.HistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;
    private final HistoryRepository history;

    public RateRepository(JdbcTemplate jdbc, HistoryRepository history) {
        this.jdbc = jdbc;
        this.history = history;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's rates, by grade then from-date (an undated rate first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT grade, pct, from_date, to_date, version FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST",
                (rs, i) -> new Rate(rs.getString("grade"), pct(rs.getBigDecimal("pct")),
                        from(rs.getObject("from_date", LocalDate.class)),
                        from(rs.getObject("to_date", LocalDate.class)), rs.getInt("version")), sheetId);
    }

    /** Set a grade's rate from a from-date (null: from the beginning of time) as a new version: the one the
     *  grade had from that date is kept as an older version (in rate_version) and the new one, in force, takes
     *  the next version number; it applies through its to-date (null: with no end). */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        Date on = from == null ? null : Date.valueOf(from);
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date)"
                + " SELECT sheet_id, grade, from_date, version, pct, to_date FROM rate"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", sheetId, grade, on);
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate_version"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", Integer.class, sheetId, grade, on);
        int version = last == null ? 1 : last + 1;
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, on);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, grade, pct, on, to == null ? null : Date.valueOf(to), version);
        history.rate(sheetId, grade, pct, from, to);
        return new Rate(grade, pct(pct), from(from), from(to), version);
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
