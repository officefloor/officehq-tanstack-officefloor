package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rates for each grade of each sheet; a grade has at most one in force per from-date (or none), the
 *  newest of its versions. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every rate on the sheet, by grade, then by from-date (the one with none first). */
    public List<Rate> onSheet(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version FROM rate WHERE sheet_id = ? ORDER BY grade, from_key",
                (rs, i) -> {
                    Date from = rs.getDate("from_date");
                    Date to = rs.getDate("to_date");
                    return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("pct"),
                            from != null ? from.toLocalDate() : null, to != null ? to.toLocalDate() : null,
                            rs.getInt("version"));
                },
                sheetId);
    }

    /** How many levels of parent sheets a grade's rate is looked for in. */
    public static final int PARENT_LEVELS = 3;

    /** The grade's rate in force on `date`: the sheet's own (see {@link #ownPct}), else its parent's
     *  then, else the parent's parent's, up to {@link #PARENT_LEVELS} levels up; null when none has one. */
    public BigDecimal pct(String sheetId, String grade, LocalDate date) {
        String sheet = sheetId;
        for (int level = 0; sheet != null && level <= PARENT_LEVELS; level++) {
            BigDecimal pct = ownPct(sheet, grade, date);
            if (pct != null) return pct;
            sheet = parent(sheet);
        }
        return null;
    }

    /** The sheet's parent sheet id, or null when it names none. */
    public String parent(String sheetId) {
        return jdbc.query("SELECT parent_id FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getString("parent_id"), sheetId)
                .stream().filter(p -> p != null).findFirst().orElse(null);
    }

    /** The sheet's ancestors' rates, nearest first: each parent sheet up to {@link #PARENT_LEVELS}
     *  levels up, with every rate set on it. */
    public List<Inherited> inherited(String sheetId) {
        List<Inherited> inherited = new ArrayList<>();
        String sheet = parent(sheetId);
        for (int level = 1; sheet != null && level <= PARENT_LEVELS; level++) {
            inherited.add(new Inherited(level, sheet, onSheet(sheet)));
            sheet = parent(sheet);
        }
        return inherited;
    }

    /** The rates of an ancestor sheet `level` levels up (1 for the parent). */
    public record Inherited(int level, String sheetId, List<Rate> rates) {
    }

    /** The grade's rate in force on `date` set on the sheet itself: the one with the latest from-date
     *  on or before it, or null when there is none or that one's to-date is before `date`. */
    public BigDecimal ownPct(String sheetId, String grade, LocalDate date) {
        return jdbc.query("SELECT pct, to_date FROM rate WHERE sheet_id = ? AND grade = ? AND from_key <= ?"
                + " ORDER BY from_key DESC LIMIT 1",
                (rs, i) -> {
                    Date to = rs.getDate("to_date");
                    return to != null && to.toLocalDate().isBefore(date) ? null : rs.getBigDecimal("pct");
                }, sheetId, grade, Date.valueOf(date))
                .stream().filter(p -> p != null).findFirst().orElse(null);
    }

    /** Set the grade's rate from its from-date as its newest version: the one it had for that from-date
     *  is kept as an older version. */
    public void save(Rate rate) {
        Date from = rate.from() != null ? Date.valueOf(rate.from()) : null;
        Integer current = jdbc.query("SELECT version FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                (rs, i) -> rs.getInt("version"), rate.sheetId(), rate.grade(), from).stream().findFirst().orElse(null);
        if (current != null) {
            jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date) "
                    + "SELECT sheet_id, grade, from_date, version, pct, to_date FROM rate "
                    + "WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                    rate.sheetId(), rate.grade(), from);
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                    rate.sheetId(), rate.grade(), from);
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version) VALUES (?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct(), from, rate.to() != null ? Date.valueOf(rate.to()) : null,
                current != null ? current + 1 : 1);
    }
}
