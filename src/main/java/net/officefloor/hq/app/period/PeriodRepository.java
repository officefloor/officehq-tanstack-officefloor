package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** The stored periods of each sheet. */
@Repository
public class PeriodRepository {

    private static final String COLUMNS = "id, sheet_id, name, from_date, to_date, closed, closed_total, soft_closed, cap, locked";

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public PeriodRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** A period as it stood at the end of a date: closed or soft-closed then, with the figures that fixed, and whether
     *  it was locked then. */
    public record StateAsOf(boolean closed, boolean soft, BigDecimal closedTotal, Map<Long, BigDecimal> personTotals,
            Map<Long, BigDecimal> personCarried, boolean locked) {

        public StateAsOf(boolean closed, boolean soft, BigDecimal closedTotal, Map<Long, BigDecimal> personTotals,
                Map<Long, BigDecimal> personCarried) {
            this(closed, soft, closedTotal, personTotals, personCarried, false);
        }

        StateAsOf withLocked(boolean locked) {
            return new StateAsOf(closed, soft, closedTotal, personTotals, personCarried, locked);
        }
    }

    /** Figures by person id from their logged text: "personId=figure" joined by ";". */
    private static Map<Long, BigDecimal> parseFigures(String text) {
        Map<Long, BigDecimal> figures = new LinkedHashMap<>();
        if (text != null && !text.isEmpty()) {
            for (String part : text.split(";")) {
                int at = part.indexOf('=');
                figures.put(Long.parseLong(part.substring(0, at)), new BigDecimal(part.substring(at + 1)).setScale(2));
            }
        }
        return figures;
    }

    /** Figures by person id as logged text (see #parseFigures). */
    private static String figuresText(Map<Long, BigDecimal> figures) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Long, BigDecimal> e : figures.entrySet()) {
            text.append(text.length() == 0 ? "" : ";").append(e.getKey()).append('=').append(e.getValue().toPlainString());
        }
        return text.toString();
    }

    /** The sheet's periods as they stood at the end of the date, by from-date, each with its state then: one added while
     *  the sheet's today was after it is left out; one never closed, soft-closed or reopened is as it is now; one is
     *  locked only when it was locked on or before the date. */
    public Map<Period, StateAsOf> listAsOf(String sheetId, LocalDate date) {
        Map<Long, LocalDate> added = new java.util.HashMap<>();
        Map<Long, LocalDate> lockedOn = new java.util.HashMap<>();
        jdbc.query("SELECT id, added_on, locked_on FROM period WHERE sheet_id = ?",
                (ResultSet rs) -> {
                    added.put(rs.getLong("id"), rs.getObject("added_on", LocalDate.class));
                    lockedOn.put(rs.getLong("id"), rs.getObject("locked_on", LocalDate.class));
                }, sheetId);
        Map<Long, StateAsOf> states = new java.util.HashMap<>();
        java.util.Set<Long> logged = new java.util.HashSet<>();
        jdbc.query("SELECT period_id, closed, soft_closed, closed_total, person_totals, person_carried, made_on FROM period_log"
                + " WHERE sheet_id = ? ORDER BY id", (ResultSet rs) -> {
                    long id = rs.getLong("period_id");
                    logged.add(id);
                    if (!AsOf.counts(rs.getObject("made_on", LocalDate.class), date)) {
                        return;
                    }
                    BigDecimal total = rs.getBigDecimal("closed_total");
                    states.put(id, new StateAsOf(rs.getBoolean("closed"), rs.getBoolean("soft_closed"),
                            total == null ? null : total.setScale(2), parseFigures(rs.getString("person_totals")),
                            parseFigures(rs.getString("person_carried"))));
                }, sheetId);
        Map<Period, StateAsOf> out = new LinkedHashMap<>();
        for (Period p : list(sheetId)) {
            if (!AsOf.counts(added.get(p.id()), date)) {
                continue;
            }
            StateAsOf state = states.get(p.id());
            if (state == null) {
                state = logged.contains(p.id()) ? new StateAsOf(false, false, null, Map.of(), Map.of())
                        : new StateAsOf(p.closed(), p.soft(), p.closedTotal(), p.closed() || p.soft() ? personTotals(p) : Map.of(),
                                p.closed() || p.soft() ? personCarried(p) : Map.of());
            }
            out.put(p, state.withLocked(p.locked() && AsOf.counts(lockedOn.get(p.id()), date)));
        }
        return out;
    }

    /** Logs the period's new state with the sheet's today. */
    private void log(Period period, boolean closed, boolean soft, BigDecimal total) {
        jdbc.update("INSERT INTO period_log (sheet_id, period_id, closed, soft_closed, closed_total, made_on) VALUES (?, ?, ?, ?, ?, ?)",
                period.sheetId(), period.id(), closed, soft, total, Date.valueOf(sheets.today(period.sheetId())));
    }

    /** The sheet's periods, by from-date. */
    public List<Period> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? ORDER BY from_date, id",
                PeriodRepository::map, sheetId);
    }

    /** Whether the name is already a period's on the sheet, case and outer spaces ignored. */
    public boolean nameTaken(String sheetId, String name) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND LOWER(name) = LOWER(?)",
                Integer.class, sheetId, name.trim());
        return n != null && n > 0;
    }

    /** Whether any period of the sheet shares a day with the dates from..to, both inclusive. */
    public boolean overlaps(String sheetId, LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                Integer.class, sheetId, Date.valueOf(to), Date.valueOf(from));
        return n != null && n > 0;
    }

    /** A period of the sheet by id, or null when it has none. */
    public Period find(String sheetId, long id) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND id = ?",
                PeriodRepository::map, sheetId, id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The period of the sheet whose dates contain the date, or null when none does. */
    public Period containing(String sheetId, LocalDate date) {
        List<Period> found = jdbc.query("SELECT " + COLUMNS + " FROM period WHERE sheet_id = ? AND from_date <= ? AND to_date >= ?",
                PeriodRepository::map, sheetId, Date.valueOf(date), Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether a closed period of the sheet contains the date. */
    public boolean closedOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND closed = TRUE "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Closes the period with its fixed due-total, but only while it is not closed (open or soft-closed), dropping the
 *  figures a soft close fixed; false when it was closed. */
    public boolean close(Period period, BigDecimal total) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_id = ?", period.sheetId(), period.id());
        boolean done = jdbc.update("UPDATE period SET closed = TRUE, soft_closed = FALSE, closed_total = ? WHERE sheet_id = ? AND id = ? AND closed = FALSE",
                total, period.sheetId(), period.id()) == 1;
        if (done) {
            log(period, true, false, total);
        }
        return done;
    }

    /** Soft-closes the period with its fixed due-total, but only while it is open; false when it was not. */
    public boolean softClose(Period period, BigDecimal total) {
        boolean done = jdbc.update("UPDATE period SET soft_closed = TRUE, closed_total = ? WHERE sheet_id = ? AND id = ? AND closed = FALSE "
                + "AND soft_closed = FALSE", total, period.sheetId(), period.id()) == 1;
        if (done) {
            log(period, false, true, total);
        }
        return done;
    }

    /** Whether a soft-closed period of the sheet contains the date. */
    public boolean softClosedOn(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND soft_closed = TRUE "
                + "AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Fixes each person's due-total and carried figure for the period, as it is closed (kept on its close's log line
     *  too). */
    public void fixPersonTotals(Period period, Map<Long, BigDecimal> totals, Map<Long, BigDecimal> carried) {
        for (Map.Entry<Long, BigDecimal> e : totals.entrySet()) {
            jdbc.update("INSERT INTO period_person_total (sheet_id, period_id, person_id, total, carried) VALUES (?, ?, ?, ?, ?)",
                    period.sheetId(), period.id(), e.getKey(), e.getValue(), carried.getOrDefault(e.getKey(), BigDecimal.ZERO.setScale(2)));
        }
        jdbc.update("UPDATE period_log SET person_totals = ?, person_carried = ? WHERE id = (SELECT MAX(id) FROM period_log"
                + " WHERE sheet_id = ? AND period_id = ?)", figuresText(totals), figuresText(carried), period.sheetId(), period.id());
    }

    /** Fixes every pool member's cut for the period, as it is closed or soft-closed, on its close's log line (see
     *  PoolRepository#liveCuts for the text). */
    public void fixPoolCuts(Period period, String cuts) {
        jdbc.update("UPDATE period_log SET pool_cuts = ? WHERE id = (SELECT MAX(id) FROM period_log"
                + " WHERE sheet_id = ? AND period_id = ?)", cuts, period.sheetId(), period.id());
    }

    /** The pool cuts fixed for each of the sheet's periods closed or soft-closed at the end of the date (null: now), by
     *  period id, as their close's log line keeps them; a period open then, or one closed before cuts were kept, is left
     *  out. */
    public Map<Long, String> poolCuts(String sheetId, LocalDate date) {
        Map<Long, String> cuts = new java.util.HashMap<>();
        jdbc.query("SELECT period_id, closed, soft_closed, pool_cuts, made_on FROM period_log WHERE sheet_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    if (date != null && !AsOf.counts(rs.getObject("made_on", LocalDate.class), date)) {
                        return;
                    }
                    long id = rs.getLong("period_id");
                    String text = rs.getString("pool_cuts");
                    if ((rs.getBoolean("closed") || rs.getBoolean("soft_closed")) && text != null) {
                        cuts.put(id, text);
                    } else {
                        cuts.remove(id);
                    }
                }, sheetId);
        return cuts;
    }

    /** The fixed carried figure of each person for the closed period, by person id (one fixed before carried figures
     *  were kept is left out). */
    public Map<Long, BigDecimal> personCarried(Period period) {
        Map<Long, BigDecimal> carried = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, carried FROM period_person_total WHERE sheet_id = ? AND period_id = ? AND carried IS NOT NULL"
                + " ORDER BY person_id", (ResultSet rs) -> {
                    carried.put(rs.getLong("person_id"), rs.getBigDecimal("carried").setScale(2));
                }, period.sheetId(), period.id());
        return carried;
    }

    /** Each person's opening balances pasted for the period added up, by person id (see OpeningRepository): as of a
     *  date (see AsOf) only those made while the sheet's today was on or before it; null for all of them. */
    public Map<Long, BigDecimal> openings(Period period, LocalDate asOf) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, amount, made_on FROM opening WHERE sheet_id = ? AND period_id = ? ORDER BY id",
                (ResultSet rs) -> {
                    if (asOf == null || AsOf.counts(rs.getObject("made_on", LocalDate.class), asOf)) {
                        totals.merge(rs.getLong("person_id"), rs.getBigDecimal("amount").setScale(2), BigDecimal::add);
                    }
                }, period.sheetId(), period.id());
        return totals;
    }

    /** The fixed due-total of each person for the closed period, by person id. */
    public Map<Long, BigDecimal> personTotals(Period period) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        jdbc.query("SELECT person_id, total FROM period_person_total WHERE sheet_id = ? AND period_id = ? ORDER BY person_id",
                (ResultSet rs) -> {
                    totals.put(rs.getLong("person_id"), rs.getBigDecimal("total").setScale(2));
                }, period.sheetId(), period.id());
        return totals;
    }

    /** Reopens the period, dropping its fixed due-totals, but only while it is closed or soft-closed; false when it was not. */
    public boolean reopen(Period period) {
        jdbc.update("DELETE FROM period_person_total WHERE sheet_id = ? AND period_id = ?", period.sheetId(), period.id());
        boolean done = jdbc.update("UPDATE period SET closed = FALSE, soft_closed = FALSE, closed_total = NULL WHERE sheet_id = ? AND id = ? "
                + "AND (closed = TRUE OR soft_closed = TRUE) AND locked = FALSE",
                period.sheetId(), period.id()) == 1;
        if (done) {
            log(period, false, false, null);
        }
        return done;
    }

    /** Locks or unlocks the period, but only while it is not already so; false when it was. */
    public boolean setLocked(Period period, boolean locked) {
        return jdbc.update("UPDATE period SET locked = ?, locked_on = ? WHERE sheet_id = ? AND id = ? AND locked = ?",
                locked, locked ? Date.valueOf(sheets.today(period.sheetId())) : null, period.sheetId(), period.id(),
                !locked) == 1;
    }

    /** Stores a new period and returns it with its id. */
    public Period add(String sheetId, String name, LocalDate from, LocalDate to, Integer cap) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update((con) -> {
            var ps = con.prepareStatement("INSERT INTO period (sheet_id, name, from_date, to_date, added_on, cap) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            ps.setDate(5, Date.valueOf(sheets.today(sheetId)));
            ps.setObject(6, cap);
            return ps;
        }, keys);
        return new Period(keys.getKey().longValue(), sheetId, name, from, to, false, null, false, cap, false);
    }

    private static Period map(ResultSet rs, int n) throws SQLException {
        BigDecimal total = rs.getBigDecimal("closed_total");
        return new Period(rs.getLong("id"), rs.getString("sheet_id"), rs.getString("name"),
                rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate(), rs.getBoolean("closed"), total == null ? null : total.setScale(2),
                rs.getBoolean("soft_closed"), rs.getObject("cap", Integer.class), rs.getBoolean("locked"));
    }
}
