package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        return jdbc.query("SELECT grade, pct, from_date, to_date, version, after_seq FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST", (rs, i) -> row(rs, false), sheetId);
    }

    /** The sheet's older (superseded) rate versions, by grade, from-date (an undated rate first) then version. */
    public List<Rate> earlier(String sheetId) {
        return jdbc.query("SELECT grade, pct, from_date, to_date, version, after_seq FROM rate_version WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST, version", (rs, i) -> row(rs, true), sheetId);
    }

    private static Rate row(ResultSet rs, boolean superseded) throws SQLException {
        return new Rate(rs.getString("grade"), pct(rs.getBigDecimal("pct")), from(rs.getObject("from_date", LocalDate.class)),
                from(rs.getObject("to_date", LocalDate.class)), rs.getInt("version"), rs.getObject("after_seq", Long.class),
                superseded);
    }

    /** Set a grade's rate from a from-date (null: from the beginning of time) as a new version: the one the
     *  grade had from that date is kept as an older version (in rate_version) and the new one, in force, takes
     *  the next version number; it applies through its to-date (null: with no end). Saved with a from-date on or
     *  after the sheet's today it is forward-only: it takes the next claim-added number (claim_added_seq), so it
     *  applies only to the claims added after it. */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        Date on = from == null ? null : Date.valueOf(from);
        List<LocalDate> today = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", LocalDate.class, sheetId);
        Long after = from != null && !today.isEmpty() && today.get(0) != null && !from.isBefore(today.get(0))
                ? jdbc.queryForObject("SELECT NEXT VALUE FOR claim_added_seq", Long.class) : null;
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, after_seq)"
                + " SELECT sheet_id, grade, from_date, version, pct, to_date, after_seq FROM rate"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", sheetId, grade, on);
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate_version"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", Integer.class, sheetId, grade, on);
        int version = last == null ? 1 : last + 1;
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, on);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, after_seq) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sheetId, grade, pct, on, to == null ? null : Date.valueOf(to), version, after);
        history.rate(sheetId, grade, pct, from, to, after);
        return new Rate(grade, pct(pct), from(from), from(to), version, after, false);
    }

    /** Withdraw a grade's rate from a from-date (null: from the beginning of time): its newest version (in rate) is
     *  removed and the version before it (the newest in rate_version), when there is one, is put back in force; with
     *  none the grade has no rate from that date. Kept in the rate history. The rate now in force from that date, or
     *  the withdrawn one with version 0 when none is left; null (nothing changed) when the grade had none. */
    public Rate withdraw(String sheetId, String grade, LocalDate from) {
        Date on = from == null ? null : Date.valueOf(from);
        List<Rate> current = jdbc.query("SELECT grade, pct, from_date, to_date, version, after_seq FROM rate"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", (rs, i) -> row(rs, false),
                sheetId, grade, on);
        if (current.isEmpty()) {
            return null;
        }
        Rate withdrawn = current.get(0);
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, on);
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate_version"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", Integer.class, sheetId, grade, on);
        history.rateWithdrawn(sheetId, grade, new BigDecimal(withdrawn.pct()), from);
        if (last == null) {
            return new Rate(withdrawn.grade(), withdrawn.pct(), withdrawn.from(), withdrawn.to(), 0, withdrawn.after(), false);
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, after_seq)"
                + " SELECT sheet_id, grade, pct, from_date, to_date, version, after_seq FROM rate_version"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ? AND version = ?",
                sheetId, grade, on, last);
        jdbc.update("DELETE FROM rate_version WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?"
                + " AND version = ?", sheetId, grade, on, last);
        return jdbc.query("SELECT grade, pct, from_date, to_date, version, after_seq FROM rate"
                + " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", (rs, i) -> row(rs, false),
                sheetId, grade, on).get(0);
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
