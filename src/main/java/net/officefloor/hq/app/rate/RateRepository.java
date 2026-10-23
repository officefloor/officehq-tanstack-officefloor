package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Rates by sheet: per grade, at most one in force per from-date (and at most one with no from-date) — the newest of
 *  the versions set for it, the older ones kept. A grade with no rate in
 *  force on a date on a sheet takes its parent sheet's rate on that date, then the parent's parent's, up to
 *  {@link #LEVELS} levels up. */
@Repository
public class RateRepository implements Rates {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class), version(rs.getObject("version", Integer.class)));

    private static int version(Integer version) {
        return version != null ? version : 1;
    }

    /** How many levels of parent sheets a rate is looked up through. */
    public static final int LEVELS = 3;

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates in force by grade (each grade and from-date's newest version), each grade's earliest first
     *  (the one with no from-date before all). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM rate r WHERE sheet_id = ? AND version = (SELECT MAX(v.version) FROM rate v "
                + "WHERE v.sheet_id = r.sheet_id AND v.grade = r.grade AND v.from_date IS NOT DISTINCT FROM r.from_date) "
                + "ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** The grade's rate in force on the date: the one whose from-date is the latest on or before it (no
     *  from-date counting as the beginning of time); empty when none applies, or that one's to-date is before the
     *  date; then the same of the sheet's parents, nearest first. */
    @Override
    public Optional<Rate> on(String sheetId, String grade, LocalDate date) {
        Optional<Rate> rate = Rates.on(list(sheetId), grade, date);
        for (String parent : parents(sheetId)) {
            if (rate.isPresent()) {
                break;
            }
            rate = Rates.on(list(parent), grade, date);
        }
        return rate;
    }

    /** The sheet's rates as they stood at the end of the day: each set from a from-date replaces the one set before
     *  it for the grade from that same date; with its parents' rates as they stood then, nearest first. */
    public Rates asOf(String sheetId, LocalDate end) {
        List<List<Rate>> levels = new ArrayList<>();
        levels.add(listAsOf(sheetId, end));
        for (String parent : parents(sheetId)) {
            levels.add(listAsOf(parent, end));
        }
        return (sheet, grade, date) -> levels.stream().map(rates -> Rates.on(rates, grade, date))
                .flatMap(Optional::stream).findFirst();
    }

    /** The sheet's parent sheets, nearest first, up to LEVELS of them (fewer when the chain ends sooner). */
    public List<String> parents(String sheetId) {
        List<String> parents = new ArrayList<>();
        String at = sheetId;
        while (parents.size() < LEVELS) {
            at = parent(at);
            if (at == null || at.equals(sheetId) || parents.contains(at)) {
                break;
            }
            parents.add(at);
        }
        return parents;
    }

    private String parent(String sheetId) {
        return jdbc.query("SELECT parent FROM sheet WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, sheetId);
    }

    /** The sheet's rates as they stood at the end of the day, by grade, each grade's earliest first (see asOf). */
    public List<Rate> listAsOf(String sheetId, LocalDate end) {
        Map<String, Rate> byGradeFrom = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM rate_log WHERE sheet_id = ? AND (made_on IS NULL OR made_on <= ?) ORDER BY id",
                rs -> {
                    Rate r = ROW.mapRow(rs, 0);
                    byGradeFrom.put(r.grade() + "|" + r.from(), r);
                }, sheetId, end);
        return byGradeFrom.values().stream().sorted(java.util.Comparator.comparing(Rate::grade)
                .thenComparing(Rate::from, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder())))
                .toList();
    }

    /** Set the grade's rate from the from-date (null: from the beginning of time) through the to-date (null:
     *  with no end) as the next version of the grade's rate from that same date, keeping any it replaces as an older
     *  version. */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to) : null;
        Integer latest = jdbc.queryForObject("SELECT MAX(version) FROM rate WHERE sheet_id = ? AND grade = ? "
                + "AND from_date IS NOT DISTINCT FROM ?", Integer.class, sheetId, grade, fromDate);
        int version = latest != null ? latest + 1 : 1;
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, grade, pct, fromDate, toDate, version);
        jdbc.update("INSERT INTO rate_log (sheet_id, grade, pct, from_date, to_date, made_on, version) VALUES (?, ?, ?, "
                + "?, ?, (SELECT today FROM sheet WHERE id = ?), ?)", sheetId, grade, pct, fromDate, toDate, sheetId,
                version);
        return new Rate(sheetId, grade, pct, from, to, version);
    }
}
