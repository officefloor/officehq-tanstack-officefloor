package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The store of rates, one per grade and from-date, kept per sheet: the newest version, with the values it replaced kept
 * as older versions in {@code rate_version}.
 */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate"),
                from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate(), rs.getInt("version"),
                rs.getObject("claims_after", Long.class));
    };

    private final JdbcTemplate jdbc;

    private final Sheets sheets;

    public Rates(JdbcTemplate jdbc, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's rates, by grade, then by from-date (one with none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM " + AsOf.table("rate") + " WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** How many sheets up from a sheet its rates are looked for when it has none in force: parent, grandparent, great-grandparent. */
    public static final int PARENT_LEVELS = 3;

    /**
     * The sheet, then its parent, its parent's parent and so on, up to {@link #PARENT_LEVELS} levels up; then, for a
     * sheet that was cloned, the lineage of the sheet it was cloned from (its parents and its own source in turn).
     */
    public List<String> lineage(String sheetId) {
        List<String> lineage = new java.util.ArrayList<>();
        String from = sheetId;
        while (from != null && !lineage.contains(from)) {
            lineage.add(from);
            String at = from;
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
            // The source is not dated either.
            List<String> source = jdbc.queryForList("SELECT source_id FROM sheet_source WHERE id = ?", String.class, from);
            from = source.isEmpty() ? null : source.get(0);
        }
        return lineage;
    }

    /**
     * Sets the rate as the newest version for the grade and from-date: the one it replaces, if any, is kept as an older
     * version. A rate with a from-date on or after the sheet's today is forward-only: it is kept with the highest claim
     * id there is now, and applies only to claims added after it. Returns the rate as stored, with its version.
     */
    public Rate save(Rate rate) {
        Date from = rate.from() == null ? null : Date.valueOf(rate.from());
        Long claimsAfter = rate.from() != null && !rate.from().isBefore(sheets.today(rate.sheetId()))
                ? jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM claim", Long.class)
                : null;
        List<Rate> replaced = jdbc.query("SELECT * FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                ROW, rate.sheetId(), rate.grade(), from);
        int version = 1;
        for (Rate old : replaced) {
            jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, rate, to_date, claims_after) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    old.sheetId(), old.grade(), from, old.version(), old.rate(), old.to() == null ? null : Date.valueOf(old.to()),
                    old.claimsAfter());
            version = Math.max(version, old.version() + 1);
        }
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version, claims_after) VALUES (?, ?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()), version,
                claimsAfter);
        return new Rate(rate.sheetId(), rate.grade(), rate.rate(), rate.from(), rate.to(), version, claimsAfter);
    }

    /**
     * Withdraws the newest version of the grade and from-date's rate: the version before it, if any, applies again (with
     * the version count it had); with none before it the rate is gone. Returns what was withdrawn, or null when the grade
     * and from-date have no rate on the sheet.
     */
    public WithdrawnRate withdraw(String sheetId, String grade, LocalDate fromDate) {
        Date from = fromDate == null ? null : Date.valueOf(fromDate);
        List<Rate> current = jdbc.query("SELECT * FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                ROW, sheetId, grade, from);
        if (current.isEmpty()) {
            return null;
        }
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", sheetId, grade, from);
        List<java.util.Map<String, Object>> older = jdbc.queryForList(
                "SELECT * FROM rate_version WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ? ORDER BY version DESC, id DESC LIMIT 1",
                sheetId, grade, from);
        if (older.isEmpty()) {
            return new WithdrawnRate(sheetId, grade, fromDate, null);
        }
        java.util.Map<String, Object> row = older.get(0);
        jdbc.update("DELETE FROM rate_version WHERE id = ?", row.get("ID"));
        Object to = row.get("TO_DATE");
        Object claimsAfter = row.get("CLAIMS_AFTER");
        Rate restored = new Rate(sheetId, grade, (BigDecimal) row.get("RATE"), fromDate,
                to == null ? null : ((Date) to).toLocalDate(), ((Number) row.get("VERSION")).intValue(),
                claimsAfter == null ? null : ((Number) claimsAfter).longValue());
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version, claims_after) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sheetId, grade, restored.rate(), from, to, restored.version(), restored.claimsAfter());
        return new WithdrawnRate(sheetId, grade, fromDate, restored);
    }

    /**
     * The grade's rate on the date: the one with the latest from-date on or before it; null when none applies, or when
     * that one's to-date is before the date (an earlier rate does not take over again). When the sheet has none, its
     * parent's rate for the grade and date is used, then the parent's parent, up to {@link #PARENT_LEVELS} levels up,
     * then the rate of the sheet it was cloned from (see {@link #lineage}).
     */
    public BigDecimal on(String sheetId, String grade, LocalDate date) {
        return on(sheetId, grade, date, null);
    }

    /**
     * The grade's rate for the claim, on its date (see {@link #on(String, String, LocalDate)}): forward-only rates saved
     * after the claim was added are passed over, as if they were not there.
     */
    public BigDecimal forClaim(Claim claim, String grade) {
        return on(claim.sheetId(), grade, claim.date(), claim.id());
    }

    private BigDecimal on(String sheetId, String grade, LocalDate date, Long claimId) {
        for (String sheet : lineage(sheetId)) {
            BigDecimal rate = own(sheet, grade, date, claimId);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The grade's rate on the date among the sheet's own rates (see {@link #on}), not its parent's. */
    private BigDecimal own(String sheetId, String grade, LocalDate date, Long claimId) {
        return list(sheetId).stream()
                .filter(r -> r.grade().equals(grade) && (r.from() == null || !r.from().isAfter(date)) && r.appliesTo(claimId))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter(r -> r.to() == null || !r.to().isBefore(date))
                .map(Rate::rate).orElse(null);
    }
}
