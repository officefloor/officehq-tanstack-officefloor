package net.officefloor.hq.app.rate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** How many levels of parent sheets a grade with no rate in force looks up. */
    public static final int PARENT_LEVELS = 3;

    /**
     * The sheet's rates, by grade and then from-date (from the start first), followed by those of the sheets it
     * inherits from, in {@link #lineage} order: see {@link Rate#inForce}.
     */
    public List<Rate> findAll(String sheetId) {
        List<Rate> rates = new ArrayList<>();
        for (String id : lineage(sheetId, this::links)) {
            rates.addAll(findOwn(id));
        }
        return rates;
    }

    /** A sheet's parent and the sheet it was cloned from (either null: none). */
    public record Links(String parent, String source) {
    }

    /**
     * The sheet and the sheets it inherits rates from, each once, in the order they are looked at: the sheet, its
     * parent, its parent's parent and so on up to {@link #PARENT_LEVELS} levels up; then, for a sheet that was cloned,
     * the sheet it was cloned from with its own lineage likewise (its parents, then the sheet it was cloned from, ...).
     */
    public static List<String> lineage(String sheetId, Function<String, Links> links) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String at = sheetId; at != null && ids.add(at); ) {
            Links own = links.apply(at);
            String up = own.parent();
            for (int level = 1; level <= PARENT_LEVELS && up != null && ids.add(up); level++) {
                up = links.apply(up).parent();
            }
            at = own.source();
        }
        return List.copyOf(ids);
    }

    /** The sheet's own rates, by grade and then from-date (from the start first). */
    public List<Rate> findOwn(String sheetId) {
        Map<String, Rate> newest = new HashMap<>();
        for (Rate version : jdbc.query("SELECT sheet_id, grade, rate, from_date, to_date, version, forward, saved"
                + " FROM rate_version WHERE sheet_id = ? ORDER BY version", RateRepository::map, sheetId)) {
            newest.compute(key(version), (k, earlier) -> version.withEarlier(earlier));
        }
        return jdbc.query("SELECT sheet_id, grade, rate, from_date, to_date, version, forward, saved FROM rate"
                + " WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", RateRepository::map, sheetId).stream()
                .map((r) -> {
                    Rate history = newest.get(key(r));
                    return r.withEarlier(history == null || history.version() != r.version() ? null : history.earlier());
                }).toList();
    }

    private static String key(Rate rate) {
        return rate.grade() + "|" + rate.from();
    }

    /** The sheet's links as the store holds them now; they are not dated, so an as-of view follows them too. */
    private Links links(String sheetId) {
        List<Links> found = jdbc.query("SELECT parent_id, cloned_from FROM PUBLIC.sheet WHERE id = ?",
                (rs, n) -> new Links(rs.getString("parent_id"), rs.getString("cloned_from")), sheetId);
        return found.isEmpty() ? new Links(null, null) : found.get(0);
    }

    /**
     * Set the grade's rate for its from-date as a new version: one it had for that from-date is kept as an older
     * version, and the new one (with its to-date) is in force; the rate as stored, with its version number.
     */
    public Rate save(Rate rate) {
        String where = rate.from() == null ? " WHERE sheet_id = ? AND grade = ? AND from_date IS NULL"
                : " WHERE sheet_id = ? AND grade = ? AND from_date = ?";
        Object[] key = rate.from() == null ? new Object[] { rate.sheetId(), rate.grade() }
                : new Object[] { rate.sheetId(), rate.grade(), rate.from() };
        List<Integer> current = jdbc.queryForList("SELECT version FROM rate" + where, Integer.class, key);
        long saved = jdbc.queryForObject("SELECT NEXT VALUE FOR added_seq", Long.class);
        Rate stored = new Rate(rate.sheetId(), rate.grade(), rate.rate(), rate.from(), rate.to(),
                current.isEmpty() ? 1 : current.get(0) + 1, rate.forward(), saved, null);
        if (current.isEmpty()) {
            jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version, forward, saved)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", stored.sheetId(), stored.grade(), stored.rate(), stored.from(),
                    stored.to(), stored.version(), stored.forward(), stored.saved());
        } else {
            Object[] args = new Object[key.length + 5];
            args[0] = stored.rate();
            args[1] = stored.to();
            args[2] = stored.version();
            args[3] = stored.forward();
            args[4] = stored.saved();
            System.arraycopy(key, 0, args, 5, key.length);
            jdbc.update("UPDATE rate SET rate = ?, to_date = ?, version = ?, forward = ?, saved = ?" + where, args);
        }
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, rate, to_date, forward, saved)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)", stored.sheetId(), stored.grade(), stored.from(), stored.version(),
                stored.rate(), stored.to(), stored.forward(), stored.saved());
        return stored;
    }

    /**
     * Withdraw the newest version of the sheet's own rate for the grade and from-date: the version before it, when there
     * is one, is in force again; with none the rate is gone. The version now in force (0: gone); -1 when there was no
     * such rate.
     */
    public int withdraw(String sheetId, String grade, LocalDate from) {
        String where = from == null ? " WHERE sheet_id = ? AND grade = ? AND from_date IS NULL"
                : " WHERE sheet_id = ? AND grade = ? AND from_date = ?";
        Object[] key = from == null ? new Object[] { sheetId, grade } : new Object[] { sheetId, grade, from };
        List<Integer> current = jdbc.queryForList("SELECT version FROM rate" + where, Integer.class, key);
        if (current.isEmpty()) {
            return -1;
        }
        jdbc.update("DELETE FROM rate_version" + where + " AND version >= ?", append(key, current.get(0)));
        List<Rate> before = jdbc.query("SELECT sheet_id, grade, rate, from_date, to_date, version, forward, saved"
                + " FROM rate_version" + where + " ORDER BY version DESC", RateRepository::map, key);
        if (before.isEmpty()) {
            jdbc.update("DELETE FROM rate" + where, key);
            return 0;
        }
        Rate restored = before.get(0);
        Object[] args = new Object[key.length + 5];
        args[0] = restored.rate();
        args[1] = restored.to();
        args[2] = restored.version();
        args[3] = restored.forward();
        args[4] = restored.saved();
        System.arraycopy(key, 0, args, 5, key.length);
        jdbc.update("UPDATE rate SET rate = ?, to_date = ?, version = ?, forward = ?, saved = ?" + where, args);
        return restored.version();
    }

    private static Object[] append(Object[] values, Object value) {
        Object[] all = new Object[values.length + 1];
        System.arraycopy(values, 0, all, 0, values.length);
        all[values.length] = value;
        return all;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2),
                rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class), rs.getInt("version"),
                rs.getBoolean("forward"), rs.getLong("saved"), null);
    }
}
