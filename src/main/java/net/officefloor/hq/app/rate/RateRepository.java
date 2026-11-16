package net.officefloor.hq.app.rate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored rates, one per grade and from-date per sheet: the newest version, the one in force, with the older versions
 *  it replaced kept in rate_version. A grade with no row has no rate. */
@Repository
public class RateRepository {

    private static final String COLUMNS = "sheet_id, grade, rate, from_date, to_date";
    private static final String VERSIONED = COLUMNS + ", version";

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final SettingRepository settings;

    public RateRepository(JdbcTemplate jdbc, SheetRepository sheets, SettingRepository settings) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.settings = settings;
    }

    /** The sheets whose rates the sheet uses, in the order looked at: itself, then its parent, the parent's parent,
     *  up to SettingRepository#PARENT_LEVELS levels up. */
    public List<String> chain(String sheetId) {
        List<String> chain = new ArrayList<>();
        chain.add(sheetId);
        chain.addAll(settings.ancestors(sheetId, SettingRepository.PARENT_LEVELS));
        return chain;
    }

    /** The sheet's rates, by grade, then from-date (one with none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT " + VERSIONED + " FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                RateRepository::map, sheetId);
    }

    /** The grade's rate in force on a date on the sheet (see own), or, when it has none, the one in force on its
     *  parent, then the parent's parent, up to three levels up; null when none of them has one. */
    public Rate find(String sheetId, String grade, LocalDate on) {
        for (String sheet : chain(sheetId)) {
            Rate rate = own(sheet, grade, on);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The grade's rate in force on a date — the latest from-date on or before it, a rate with none applying
     *  from the beginning of time — or null when no rate applies, or that rate's to-date is before the date. */
    public Rate own(String sheetId, String grade, LocalDate on) {
        List<Rate> found = jdbc.query("SELECT " + VERSIONED + " FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1",
                RateRepository::map, sheetId, grade, Date.valueOf(on));
        if (found.isEmpty() || (found.get(0).to() != null && found.get(0).to().isBefore(on))) {
            return null;
        }
        return found.get(0);
    }

    /** Every rate stored on the sheet while its today was on or before the date, in the order stored. */
    public List<Rate> storedBy(String sheetId, LocalDate madeOn) {
        return jdbc.query("SELECT " + COLUMNS + " FROM rate_log WHERE sheet_id = ? AND made_on <= ? ORDER BY id",
                RateRepository::logged, sheetId, Date.valueOf(madeOn));
    }

    /** Stores the rate as the newest version of the grade's rate for its from-date: the one it had for that from-date
     *  is kept as an older version, the new one numbered after it (1 when it had none). Logs it with the sheet's today. */
    public Rate save(Rate set) {
        Date from = set.from() == null ? null : Date.valueOf(set.from());
        String same = " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?";
        jdbc.update("INSERT INTO rate_version (" + VERSIONED + ") SELECT " + VERSIONED + " FROM rate" + same,
                set.sheetId(), set.grade(), from);
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate" + same, Integer.class,
                set.sheetId(), set.grade(), from);
        Rate rate = set.withVersion(last == null ? 1 : last + 1);
        jdbc.update("DELETE FROM rate" + same, rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (" + VERSIONED + ") VALUES (?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()),
                rate.version());
        jdbc.update("INSERT INTO rate_log (" + COLUMNS + ", made_on) VALUES (?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()),
                Date.valueOf(sheets.today(rate.sheetId())));
        return rate;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return logged(rs, n).withVersion(rs.getInt("version"));
    }

    /** A rate as logged, which keeps no version (see SheetAsOf for how it counts them). */
    private static Rate logged(ResultSet rs, int n) throws SQLException {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2),
                from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate());
    }
}
