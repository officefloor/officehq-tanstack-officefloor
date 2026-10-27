package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.setting.Settings;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of rates, one per grade and from-date, kept per sheet. */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate"),
                from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate());
    };

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade, then by from-date (one with none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM " + AsOf.table("rate") + " WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** How many sheets up from a sheet its rates are looked for when it has none in force: parent, grandparent, great-grandparent. */
    public static final int PARENT_LEVELS = 3;

    /** The sheet, then its parent, its parent's parent and so on, up to {@link #PARENT_LEVELS} levels up. */
    public List<String> lineage(String sheetId) {
        List<String> lineage = new java.util.ArrayList<>(List.of(sheetId));
        String at = sheetId;
        for (int level = 0; level < PARENT_LEVELS; level++) {
            // The parent is not dated: it is the one named now, even while the store is read as of a date.
            List<String> parent = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet_id = ? AND setting_key = ?",
                    String.class, at, Settings.PARENT);
            if (parent.isEmpty() || parent.get(0) == null || parent.get(0).isEmpty() || lineage.contains(parent.get(0))) {
                break;
            }
            at = parent.get(0);
            lineage.add(at);
        }
        return lineage;
    }

    /** Sets the rate, replacing any the grade already has for the same from-date. */
    public void save(Rate rate) {
        Date from = rate.from() == null ? null : Date.valueOf(rate.from());
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()));
    }

    /**
     * The grade's rate on the date: the one with the latest from-date on or before it; null when none applies, or when
     * that one's to-date is before the date (an earlier rate does not take over again). When the sheet has none, its
     * parent's rate for the grade and date is used, then the parent's parent, up to {@link #PARENT_LEVELS} levels up.
     */
    public BigDecimal on(String sheetId, String grade, LocalDate date) {
        for (String sheet : lineage(sheetId)) {
            BigDecimal rate = own(sheet, grade, date);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The grade's rate on the date among the sheet's own rates (see {@link #on}), not its parent's. */
    private BigDecimal own(String sheetId, String grade, LocalDate date) {
        return list(sheetId).stream()
                .filter(r -> r.grade().equals(grade) && (r.from() == null || !r.from().isAfter(date)))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter(r -> r.to() == null || !r.to().isBefore(date))
                .map(Rate::rate).orElse(null);
    }
}
