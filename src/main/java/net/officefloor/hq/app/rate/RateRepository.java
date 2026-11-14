package net.officefloor.hq.app.rate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
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
     * The sheet's rates, by grade and then from-date (from the start first), followed by those of its parent, its
     * parent's parent and so on, up to {@link #PARENT_LEVELS} levels up: see {@link Rate#inForce}.
     */
    public List<Rate> findAll(String sheetId) {
        List<Rate> rates = new ArrayList<>(findOwn(sheetId));
        Set<String> seen = new HashSet<>(Set.of(sheetId));
        String up = parent(sheetId);
        for (int level = 1; level <= PARENT_LEVELS && up != null && seen.add(up); level++) {
            rates.addAll(findOwn(up));
            up = parent(up);
        }
        return rates;
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

    private String parent(String sheetId) {
        List<String> parent = jdbc.query("SELECT parent_id FROM sheet WHERE id = ?", (rs, n) -> rs.getString("parent_id"), sheetId);
        return parent.isEmpty() ? null : parent.get(0);
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

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2),
                rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class), rs.getInt("version"),
                rs.getBoolean("forward"), rs.getLong("saved"), null);
    }
}
