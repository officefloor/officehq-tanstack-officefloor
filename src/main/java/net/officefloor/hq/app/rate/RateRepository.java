package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The rates for each grade on each sheet, each in force from its from-date (null: from the beginning
 * of time), through its to-date (null: with no end). A grade without one in force on a date has no rate on
 * that date on the sheet itself; then its parent's rate on that date is used, then the parent's parent's, up to
 * three levels up (see {@link #chain}).
 */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate"),
                from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate());
    };

    /** How many levels of parents a sheet takes rates from. */
    public static final int PARENT_LEVELS = 3;

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade, then by from-date (none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, rate, from_date, to_date FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** The sheet, then its parent, its parent's parent, and so on up to {@link #PARENT_LEVELS} levels up. */
    public List<String> ancestry(String sheetId) {
        List<String> found = new java.util.ArrayList<>(List.of(sheetId));
        String at = sheetId;
        while (found.size() <= PARENT_LEVELS) {
            String parent = parentOf(at);
            if (parent == null || found.contains(parent)) {
                break;
            }
            found.add(parent);
            at = parent;
        }
        return found;
    }

    /** The sheet's parent; null when it has none. */
    public String parentOf(String sheetId) {
        return jdbc.query("SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = 'parent'",
                (rs, n) -> rs.getString(1), sheetId).stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
    }

    /** The sheets naming this one as their parent, by id. */
    public List<String> childrenOf(String sheetId) {
        return jdbc.query("SELECT sheet_id FROM sheet_setting WHERE setting_key = 'parent' AND setting_value = ?"
                + " ORDER BY sheet_id", (rs, n) -> rs.getString(1), sheetId);
    }

    /** The sheet's children, their children, and so on down (each once, the sheet itself never among them). */
    public List<String> descendantsOf(String sheetId) {
        List<String> found = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        java.util.Deque<String> todo = new java.util.ArrayDeque<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            for (String child : childrenOf(todo.poll())) {
                if (seen.add(child)) {
                    found.add(child);
                    todo.add(child);
                }
            }
        }
        return found;
    }

    /** The sheet's own rates (level 0), then each of its parents' (see {@link #ancestry}), each at its level up. */
    public List<Rate> chain(String sheetId) {
        List<String> sheets = ancestry(sheetId);
        List<Rate> found = new java.util.ArrayList<>();
        for (int level = 0; level < sheets.size(); level++) {
            int at = level;
            list(sheets.get(level)).forEach(r -> found.add(r.at(at)));
        }
        return found;
    }

    /**
     * The grade's rate in force on the date, on the sheet or else (none in force there) up its parents (see
     * {@link #ancestry}); null when none has one.
     */
    public BigDecimal find(String sheetId, String grade, LocalDate on) {
        for (String sheet : ancestry(sheetId)) {
            BigDecimal rate = findOwn(sheet, grade, on);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /**
     * The grade's rate in force on the date on the sheet itself: the one with the latest from-date on or before it, a rate
     * without a from-date counting as the earliest. Null when none applies, or the date is after that one's to-date.
     */
    public BigDecimal findOwn(String sheetId, String grade, LocalDate on) {
        return jdbc.query("SELECT sheet_id, grade, rate, from_date, to_date FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1", ROW,
                sheetId, grade, Date.valueOf(on)).stream().findFirst()
                .filter(r -> r.to() == null || !r.to().isBefore(on)).map(Rate::rate).orElse(null);
    }

    /**
     * Set a grade's rate from a date (null: from the beginning of time) through a to-date (null: with no end),
     * replacing one already there for that from-date.
     */
    public Rate set(String sheetId, String grade, BigDecimal rate, LocalDate from, LocalDate to) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, fromDate);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date) VALUES (?, ?, ?, ?, ?)", sheetId,
                grade, rate, fromDate, to == null ? null : Date.valueOf(to));
        return new Rate(sheetId, grade, rate, from, to);
    }
}
