package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Periods by sheet, earliest first. */
@Repository
public class PeriodRepository {

    private static final RowMapper<Period> ROW = (rs, i) -> new Period(rs.getString("sheet_id"),
            rs.getString("name"), rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class),
            rs.getBoolean("closed"), rs.getBigDecimal("closed_total"), rs.getBoolean("soft"),
            (Integer) rs.getObject("open_cap", Integer.class));

    private final JdbcTemplate jdbc;

    public PeriodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
    }

    /** Lock the sheet's row until the transaction ends, so concurrent period adds are checked one after the other. */
    public void lockSheet(String sheetId) {
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", rs -> null, sheetId);
    }

    public Period insert(String sheetId, String name, LocalDate from, LocalDate to) {
        return insert(sheetId, name, from, to, null);
    }

    /** Store a new open period with the cap on its people's OPEN or PENDING claims (null: none). */
    public Period insert(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, open_cap, added_on) VALUES (?, ?, ?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", sheetId, name, Date.valueOf(from), Date.valueOf(to), cap, sheetId);
        return new Period(sheetId, name, from, to, false, null, false, cap);
    }

    /** The period on the sheet whose dates contain the date, or null when none does. */
    public Period on(String sheetId, LocalDate date) {
        return list(sheetId).stream().filter(p -> p.contains(date)).findFirst().orElse(null);
    }

    /** The period on the sheet with this name (without regard to case or surrounding spaces), or null. */
    public Period find(String sheetId, String name) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name_key = LOWER(TRIM(?))", ROW, sheetId, name)
                .stream().findFirst().orElse(null);
    }

    /** The closed period on the sheet whose dates contain the date, or null when none does. */
    public Period closedOn(String sheetId, LocalDate date) {
        return list(sheetId).stream().filter(p -> p.closed() && p.contains(date)).findFirst().orElse(null);
    }

    /** The period on the sheet closed by hand (not automatically) that would hold a new claim dated on the date and
     *  due by the due-by: the one whose dates contain its due-by, else one it would run across (the period starts
     *  after its date and ends before its due-by); null when none does. */
    public Period closedByHandFor(String sheetId, LocalDate date, LocalDate dueBy) {
        var closed = jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND closed AND NOT auto_closed ORDER BY from_date",
                ROW, sheetId);
        return closed.stream().filter(p -> p.contains(dueBy)).findFirst()
                .or(() -> closed.stream().filter(p -> p.from().isAfter(date) && p.to().isBefore(dueBy)).findFirst())
                .orElse(null);
    }

    /** Mark the closed period as closed automatically. */
    public void markAutoClosed(Period period) {
        jdbc.update("UPDATE period SET auto_closed = TRUE WHERE sheet_id = ? AND name = ? AND closed",
                period.sheetId(), period.name());
    }

    /** The closed period on the sheet whose dates overlap from..to (both included), or null when none does. */
    public Period closedBetween(String sheetId, LocalDate from, LocalDate to) {
        return list(sheetId).stream().filter(p -> p.closed() && p.overlaps(from, to)).findFirst().orElse(null);
    }

    /** The soft-closed period on the sheet whose dates contain the date, or null when none does. */
    public Period softClosedOn(String sheetId, LocalDate date) {
        return list(sheetId).stream().filter(p -> p.soft() && p.contains(date)).findFirst().orElse(null);
    }

    /** Close the period (open or soft-closed), fixing its due-total and each person's due-total and carried figure
     *  (by person id) afresh: false when it was already closed, and nothing is written. */
    public boolean close(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried) {
        if (jdbc.update("UPDATE period SET closed = TRUE, soft = FALSE, auto_closed = FALSE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed",
                total, period.sheetId(), period.name()) != 1) {
            return false;
        }
        fixPersonTotals(period, personTotals, personCarried);
        kept(period);
        return true;
    }

    /** Soft-close the open period, fixing its due-total and each person's due-total and carried figure (by person
     *  id): false when it was already closed or soft-closed, and nothing is written. */
    public boolean softClose(Period period, BigDecimal total, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried) {
        if (jdbc.update("UPDATE period SET soft = TRUE, closed_total = ? WHERE sheet_id = ? AND name = ? AND NOT closed AND NOT soft",
                total, period.sheetId(), period.name()) != 1) {
            return false;
        }
        fixPersonTotals(period, personTotals, personCarried);
        kept(period);
        return true;
    }

    private void fixPersonTotals(Period period, Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", period.sheetId(), period.name());
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                period.sheetId(), period.name(), personId, personTotal,
                personCarried.getOrDefault(personId, new BigDecimal("0.00"))));
    }

    /** The people's due-totals fixed in the sheet's closed periods: by period name, then by person id. */
    public Map<String, Map<Long, BigDecimal>> personTotals(String sheetId) {
        Map<String, Map<Long, BigDecimal>> totals = new HashMap<>();
        jdbc.query("SELECT period_name, person_id, total FROM period_person_total WHERE sheet_id = ?", rs -> {
            totals.computeIfAbsent(rs.getString("period_name"), k -> new HashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("total"));
        }, sheetId);
        return totals;
    }

    /** The people's carried figures fixed in the sheet's closed periods: by period name, then by person id (none for
     *  a person whose figure was not fixed). */
    public Map<String, Map<Long, BigDecimal>> personCarried(String sheetId) {
        Map<String, Map<Long, BigDecimal>> carried = new HashMap<>();
        jdbc.query("SELECT period_name, person_id, carried FROM period_person_total WHERE sheet_id = ? "
                + "AND carried IS NOT NULL", rs -> {
            carried.computeIfAbsent(rs.getString("period_name"), k -> new HashMap<>())
                    .put(rs.getLong("person_id"), rs.getBigDecimal("carried"));
        }, sheetId);
        return carried;
    }

    /** Reopen the closed or soft-closed period, dropping its fixed due-totals: false when it was neither, and nothing
     *  is written. */
    public boolean reopen(Period period) {
        if (jdbc.update("UPDATE period SET closed = FALSE, soft = FALSE, auto_closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ? AND (closed OR soft)",
                period.sheetId(), period.name()) != 1) {
            return false;
        }
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", period.sheetId(), period.name());
        kept(period);
        return true;
    }

    /** Date the period's latest kept history (an auto close: the day its grace ran out, not the new today). */
    public void dateLastKept(Period period, LocalDate madeOn) {
        jdbc.update("UPDATE period_history SET made_on = ? WHERE id = (SELECT MAX(id) FROM period_history "
                + "WHERE sheet_id = ? AND period_name = ?)", madeOn, period.sheetId(), period.name());
    }

    /** Keep the period as it stands now (closed or soft-closed, and its fixed due-totals) in its history, with the
     *  sheet's today. */
    private void kept(Period period) {
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO period_history (sheet_id, period_name, closed, soft, closed_total, "
                    + "made_on) SELECT p.sheet_id, p.name, p.closed, p.soft, p.closed_total, s.today FROM period p "
                    + "JOIN sheet s ON s.id = p.sheet_id WHERE p.sheet_id = ? AND p.name = ?", new String[] { "id" });
            ps.setString(1, period.sheetId());
            ps.setString(2, period.name());
            return ps;
        }, keys);
        jdbc.update("INSERT INTO period_history_total (history_id, person_id, total, carried) SELECT ?, person_id, total, carried "
                + "FROM period_person_total WHERE sheet_id = ? AND period_name = ?", keys.getKey().longValue(),
                period.sheetId(), period.name());
    }

    /** A period as it stood at the end of a day, with the people's due-totals and carried figures fixed in it then,
     *  by person id. */
    public record PeriodThen(Period period, Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried) {
    }

    /** The sheet's periods as they stood at the end of the day, earliest first: those added while the sheet's today
     *  was on or before it (or before history was kept), each closed or soft-closed as its last close, soft-close or
     *  reopen made by then left it (open when none was). */
    public List<PeriodThen> listAsOf(String sheetId, LocalDate end) {
        return jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND (added_on IS NULL OR added_on <= ?) "
                + "ORDER BY from_date", ROW, sheetId, end).stream().map(p -> {
                    var last = jdbc.queryForList("SELECT id, closed, soft, closed_total FROM period_history "
                            + "WHERE sheet_id = ? AND period_name = ? AND (made_on IS NULL OR made_on <= ?) "
                            + "ORDER BY id DESC LIMIT 1", sheetId, p.name(), end);
                    if (last.isEmpty()) {
                        return new PeriodThen(new Period(sheetId, p.name(), p.from(), p.to(), false, null, false, p.cap()), Map.of(), Map.of());
                    }
                    var row = last.get(0);
                    Map<Long, BigDecimal> totals = new HashMap<>();
                    Map<Long, BigDecimal> carried = new HashMap<>();
                    jdbc.query("SELECT person_id, total, carried FROM period_history_total WHERE history_id = ?", rs -> {
                        totals.put(rs.getLong(1), rs.getBigDecimal(2));
                        if (rs.getBigDecimal(3) != null) {
                            carried.put(rs.getLong(1), rs.getBigDecimal(3));
                        }
                    }, row.get("id"));
                    return new PeriodThen(new Period(sheetId, p.name(), p.from(), p.to(), Boolean.TRUE.equals(row.get("closed")),
                            (BigDecimal) row.get("closed_total"), Boolean.TRUE.equals(row.get("soft")), p.cap()), totals, carried);
                }).toList();
    }
}
