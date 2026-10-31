package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The period store: named, non-overlapping date ranges (both ends inclusive), kept per sheet. */
@Repository
public class Periods {

    private static final RowMapper<PeriodResponse> ROW = (rs, i) -> {
        BigDecimal total = rs.getBigDecimal("closed_total");
        return new PeriodResponse(rs.getString("name"), rs.getDate("from_date").toLocalDate().toString(),
                rs.getDate("to_date").toLocalDate().toString(), rs.getBoolean("closed"),
                total == null ? "" : total.setScale(2).toPlainString(), new LinkedHashMap<>(),
                rs.getBoolean("soft_closed"), rs.getObject("claim_cap") == null ? "" : String.valueOf(rs.getInt("claim_cap")),
                new LinkedHashMap<>(), rs.getBoolean("locked"));
    };

    private final JdbcTemplate jdbc;

    public Periods(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's periods, by from-date. */
    public List<PeriodResponse> list(String sheetId) {
        List<PeriodResponse> periods = jdbc.query("SELECT * FROM period WHERE sheet_id = ? ORDER BY from_date", ROW, sheetId);
        Map<String, PeriodResponse> byName = new HashMap<>();
        periods.forEach(p -> byName.put(p.name(), p));
        jdbc.query("SELECT period_name, person_id, total, carried FROM period_person_total WHERE sheet_id = ? ORDER BY person_id",
                (RowCallbackHandler) rs -> {
                    PeriodResponse period = byName.get(rs.getString("period_name"));
                    if (period != null) {
                        period.closedPersonTotals().put(String.valueOf(rs.getLong("person_id")),
                                rs.getBigDecimal("total").setScale(2).toPlainString());
                        BigDecimal carried = rs.getBigDecimal("carried");
                        if (carried != null) {
                            period.closedPersonCarried().put(String.valueOf(rs.getLong("person_id")),
                                    carried.setScale(2).toPlainString());
                        }
                    }
                }, sheetId);
        return periods;
    }

    /** The named period, its row locked until the transaction ends; null when none. */
    public PeriodResponse lock(String sheetId, String name) {
        List<PeriodResponse> found = jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND name = ? FOR UPDATE",
                ROW, sheetId, name);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Close (or soft-close) the named period, fixing its due-total, each person's due-total and carried figure for it,
     *  and every pool member's cut as it stands now (see {@code Pools#frozenCuts}). */
    public void close(String sheetId, String name, BigDecimal total, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried, boolean soft, String poolCuts) {
        jdbc.update("UPDATE period SET closed = ?, soft_closed = ?, closed_total = ? WHERE sheet_id = ? AND name = ?",
                !soft, soft, total, sheetId, name);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        personTotals.forEach((personId, personTotal) -> jdbc.update(
                "INSERT INTO period_person_total (sheet_id, period_name, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                sheetId, name, personId, personTotal, personCarried.getOrDefault(personId, BigDecimal.ZERO.setScale(2))));
        history(sheetId, name, !soft, soft, total, personTotals, personCarried, poolCuts);
    }

    /** Records a close, soft-close or reopen of the period in its history, dated the sheet's today, with the pool
     *  members' cuts it fixed (null on a reopen). */
    private void history(String sheetId, String name, boolean closed, boolean soft, BigDecimal total,
            Map<Long, BigDecimal> personTotals, Map<Long, BigDecimal> personCarried, String poolCuts) {
        jdbc.update("INSERT INTO period_history (sheet_id, name, set_on, closed, soft_closed, closed_total, person_totals,"
                + " person_carried, pool_cuts) VALUES (?, ?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?, ?, ?, ?)", sheetId,
                name, sheetId, closed, soft, total, joined(personTotals), joined(personCarried), poolCuts);
    }

    /** Figures by person id as "<person id>=<figure>" joined by ";". */
    private static String joined(Map<Long, BigDecimal> figures) {
        StringBuilder joined = new StringBuilder();
        figures.forEach((personId, figure) -> joined.append(joined.isEmpty() ? "" : ";").append(personId)
                .append('=').append(figure.setScale(2).toPlainString()));
        return joined.toString();
    }

    /** Reopen the named period, dropping its fixed due-total. */
    public void reopen(String sheetId, String name) {
        jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND name = ?",
                sheetId, name);
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        history(sheetId, name, false, false, null, Map.of(), Map.of(), null);
    }

    /** Remove the named period with its fixed figures and history. */
    public void remove(String sheetId, String name) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_name = ?", sheetId, name);
        jdbc.update("DELETE FROM period_history WHERE sheet_id = ? AND name = ?", sheetId, name);
        jdbc.update("DELETE FROM period WHERE sheet_id = ? AND name = ?", sheetId, name);
    }

    /** Lock or unlock the named period. */
    public void setLocked(String sheetId, String name, boolean locked) {
        jdbc.update("UPDATE period SET locked = ? WHERE sheet_id = ? AND name = ?", locked, sheetId, name);
    }

    /** Whether a date falls inside a closed period of the sheet. */
    public boolean isClosed(String sheetId, LocalDate date) {
        Integer closed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return closed != null && closed > 0;
    }

    /** Whether a date falls inside a soft-closed period of the sheet. */
    public boolean isSoftClosed(String sheetId, LocalDate date) {
        Integer soft = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND soft_closed AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return soft != null && soft > 0;
    }

    /** The name of the sheet's period whose dates contain the date; null when none does. */
    public String nameOn(String sheetId, LocalDate date) {
        List<String> found = jdbc.query("SELECT name FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                (rs, i) -> rs.getString(1), sheetId, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The capped period whose dates contain the date, its row locked until the transaction ends (so two
     *  claims opened at once cannot both pass its cap); null when no period contains it or that one has no cap. */
    public PeriodResponse cappedOn(String sheetId, LocalDate date) {
        List<PeriodResponse> found = jdbc.query("SELECT * FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?"
                + " AND claim_cap IS NOT NULL FOR UPDATE", ROW, sheetId, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Add a period, refusing a name already on the sheet or dates overlapping another period. */
    public void add(String sheetId, ValidPeriod period) throws InvalidPeriodException {
        // Lock the sheet's row so two periods added at once cannot both pass the overlap check.
        jdbc.query("SELECT id FROM sheet WHERE id = ? FOR UPDATE", (rs, i) -> rs.getString(1), sheetId);
        Integer named = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND name = ?",
                Integer.class, sheetId, period.name());
        if (named != null && named > 0) {
            throw new InvalidPeriodException("A period named " + period.name() + " is already on the sheet");
        }
        Integer overlapping = jdbc.queryForObject(
                "SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(period.to()), Date.valueOf(period.from()));
        if (overlapping != null && overlapping > 0) {
            throw new InvalidPeriodException("The period's dates overlap another period");
        }
        try {
            jdbc.update("INSERT INTO period (sheet_id, name, from_date, to_date, added_on, claim_cap) VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?), ?)",
                    sheetId, period.name(), Date.valueOf(period.from()), Date.valueOf(period.to()), sheetId, period.cap());
        } catch (DuplicateKeyException raced) {
            throw new InvalidPeriodException("A period named " + period.name() + " is already on the sheet");
        }
    }

    /** Add a period unless its name is already on the sheet or its dates overlap another period:
     *  whether it was added. */
    public boolean addIfFree(String sheetId, ValidPeriod period) {
        try {
            add(sheetId, period);
            return true;
        } catch (InvalidPeriodException taken) {
            return false;
        }
    }
}
