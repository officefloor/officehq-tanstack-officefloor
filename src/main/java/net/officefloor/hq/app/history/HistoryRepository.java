package net.officefloor.hq.app.history;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.Hold;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The sheet's history (see V48__history.sql): each change that a statement taken as of a past date
 *  depends on, kept with the sheet's today when it was made. The repositories that make these changes
 *  record them here, in the same transaction. */
@Repository
public class HistoryRepository {

    /** The sheet's today, for the on_date of a change. */
    private static final String TODAY = "(SELECT s.today FROM sheet s WHERE s.id = ?)";

    private final JdbcTemplate jdbc;

    public HistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Keep a claim's values as they are now, after a change to it. */
    public void claim(String sheetId, long claimId) {
        jdbc.update("INSERT INTO claim_history (sheet_id, on_date, claim_id, person_id, amount, claim_date, status,"
                + " settled_due, stated, stated_due, stated_na, corrects, due_by, deferred_until, waived, held)"
                + " SELECT c.sheet_id, s.today, c.id, c.person_id, c.amount, c.claim_date, c.status, c.settled_due,"
                + " c.stated, c.stated_due, c.stated_na, c.corrects, c.due_by, c.deferred_until, c.waived, c.held"
                + " FROM claim c JOIN sheet s ON s.id = c.sheet_id WHERE c.id = ? AND c.sheet_id = ?", claimId, sheetId);
    }

    /** Keep a rate set for a grade from a from-date (null: from the beginning of time). */
    public void rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        rate(sheetId, grade, pct, from, to, null);
    }

    /** Keep a rate set for a grade from a from-date (null: from the beginning of time); {@code after} is a
     *  forward-only rate's claim-added number (null: it applies to every claim). */
    public void rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, Long after) {
        jdbc.update("INSERT INTO rate_history (sheet_id, on_date, grade, pct, from_date, to_date, after_seq) VALUES (?, " + TODAY
                + ", ?, ?, ?, ?, ?)", sheetId, sheetId, grade, pct, date(from), date(to), after);
    }

    /** Keep a rate withdrawn for a grade from a from-date (null: from the beginning of time): it takes back the last
     *  rate set from that date ({@code pct} the withdrawn one's). */
    public void rateWithdrawn(String sheetId, String grade, BigDecimal pct, LocalDate from) {
        jdbc.update("INSERT INTO rate_history (sheet_id, on_date, grade, pct, from_date, withdrawn) VALUES (?, " + TODAY
                + ", ?, ?, ?, TRUE)", sheetId, sheetId, grade, pct, date(from));
    }

    /** Keep a grade given to a person from a from-date (null: from the beginning of time). */
    public void grade(String sheetId, long personId, String grade, LocalDate from) {
        jdbc.update("INSERT INTO grade_history (sheet_id, on_date, person_id, grade, from_date) VALUES (?, " + TODAY
                + ", ?, ?, ?)", sheetId, sheetId, personId, grade, date(from));
    }

    /** Keep a dated setting's value recorded from a from-date (null: blank). */
    public void setting(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("INSERT INTO setting_history (sheet_id, on_date, setting_key, from_date, setting) VALUES (?, " + TODAY
                + ", ?, ?, ?)", sheetId, sheetId, key, date(from), value);
    }

    /** Keep the sheet's policy date set (null: blank). */
    public void policy(String sheetId, LocalDate policyDate) {
        jdbc.update("INSERT INTO policy_history (sheet_id, on_date, policy_date) VALUES (?, " + TODAY + ", ?)",
                sheetId, sheetId, date(policyDate));
    }

    /** Keep a person's retirement from a date. */
    public void retire(String sheetId, long personId, LocalDate from) {
        jdbc.update("INSERT INTO retire_history (sheet_id, on_date, person_id, retired_from) VALUES (?, " + TODAY
                + ", ?, ?)", sheetId, sheetId, personId, date(from));
    }

    private static Date date(LocalDate d) {
        return d == null ? null : Date.valueOf(d);
    }

    /** Whether the person was on the sheet at the end of a day: added while the sheet's today was on or before it. */
    public boolean existed(String sheetId, long personId, LocalDate day) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ? AND added_on <= ?",
                Integer.class, sheetId, personId, Date.valueOf(day));
        return n != null && n > 0;
    }

    /** The sheet as it stood at the end of a day: the changes made while its today was on or before that day,
     *  each kept value the last of them (in the order made). */
    public SheetAsOf asOf(String sheetId, LocalDate day) {
        Date on = Date.valueOf(day);
        Map<Long, Claim> claims = new LinkedHashMap<>();
        jdbc.query("SELECT h.* FROM claim_history h WHERE h.sheet_id = ? AND h.id = (SELECT MAX(l.id) FROM claim_history l"
                + " WHERE l.sheet_id = h.sheet_id AND l.claim_id = h.claim_id AND l.on_date <= ?) ORDER BY h.claim_id",
                (RowCallbackHandler) rs -> {
                    String status = rs.getString("status");
                    LocalDate deferredUntil = local(rs.getDate("deferred_until"));
                    BigDecimal settled = rs.getBigDecimal("settled_due");
                    BigDecimal stated = rs.getBigDecimal("stated_due");
                    claims.put(rs.getLong("claim_id"), new Claim(rs.getLong("claim_id"), rs.getLong("person_id"),
                            rs.getBigDecimal("amount").setScale(2).toPlainString(), local(rs.getDate("claim_date")).toString(),
                            "", status, null, settled == null ? null : settled.setScale(2).toPlainString(), null,
                            rs.getBoolean("stated"), stated == null ? null : stated.setScale(2).toPlainString(),
                            rs.getString("corrects"), local(rs.getDate("due_by")).toString(), rs.getBoolean("stated_na"),
                            deferredUntil == null ? null : deferredUntil.toString(),
                            (status.equals("OPEN") || status.equals("SETTLED")) && !rs.getBoolean("stated") && deferredUntil != null && deferredUntil.isAfter(day), 0, null, false, null, null, null, null,
                            rs.getBoolean("waived"), 0, rs.getBoolean("held")));
                }, sheetId, on);
        Map<Long, TreeMap<LocalDate, String>> grades = new HashMap<>();
        jdbc.query("SELECT person_id, grade, from_date FROM grade_history WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                (RowCallbackHandler) rs -> grades.computeIfAbsent(rs.getLong("person_id"), k -> new TreeMap<>())
                        .put(from(rs.getDate("from_date")), rs.getString("grade")), sheetId, on);
        Map<String, TreeMap<LocalDate, List<SheetAsOf.Rate>>> rates = new HashMap<>();
        jdbc.query("SELECT grade, pct, from_date, to_date, after_seq, withdrawn FROM rate_history WHERE sheet_id = ? AND on_date <= ?"
                + " ORDER BY id", (RowCallbackHandler) rs -> {
                    TreeMap<LocalDate, List<SheetAsOf.Rate>> dated = rates.computeIfAbsent(rs.getString("grade"), k -> new TreeMap<>());
                    LocalDate from = from(rs.getDate("from_date"));
                    if (rs.getBoolean("withdrawn")) {
                        // A withdrawal takes back the last rate set from that date.
                        List<SheetAsOf.Rate> sets = dated.get(from);
                        if (sets != null && !sets.isEmpty()) sets.remove(sets.size() - 1);
                        if (sets != null && sets.isEmpty()) dated.remove(from);
                        return;
                    }
                    dated.computeIfAbsent(from, k -> new ArrayList<>()).add(new SheetAsOf.Rate(rs.getBigDecimal("pct"),
                            local(rs.getDate("to_date")), rs.getObject("after_seq", Long.class)));
                }, sheetId, on);
        Map<Long, Long> added = new HashMap<>();
        jdbc.query("SELECT id, added_seq FROM claim WHERE sheet_id = ?",
                (RowCallbackHandler) rs -> added.put(rs.getLong("id"), rs.getLong("added_seq")), sheetId);
        Map<String, TreeMap<LocalDate, String>> settings = new HashMap<>();
        jdbc.query("SELECT setting_key, from_date, setting FROM setting_history WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                (RowCallbackHandler) rs -> settings.computeIfAbsent(rs.getString("setting_key"), k -> new TreeMap<>())
                        .put(from(rs.getDate("from_date")), rs.getString("setting")), sheetId, on);
        List<Date> policy = jdbc.query("SELECT policy_date FROM policy_history WHERE sheet_id = ? AND on_date <= ?"
                + " ORDER BY id DESC LIMIT 1", (rs, i) -> rs.getDate("policy_date"), sheetId, on);
        Map<Long, List<Hold>> holds = new HashMap<>();
        jdbc.query("SELECT person_id, from_date, to_date FROM person_hold WHERE sheet_id = ? AND held_on <= ? ORDER BY id",
                (RowCallbackHandler) rs -> holds.computeIfAbsent(rs.getLong("person_id"), k -> new ArrayList<>())
                        .add(new Hold(text(rs.getDate("from_date")), text(rs.getDate("to_date")))), sheetId, on);
        Map<Long, String> retired = new HashMap<>();
        jdbc.query("SELECT person_id, retired_from FROM retire_history WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                (RowCallbackHandler) rs -> retired.put(rs.getLong("person_id"), text(rs.getDate("retired_from"))), sheetId, on);
        return new SheetAsOf(day, claims, grades, rates, settings, policy.isEmpty() ? null : local(policy.get(0)), holds,
                retired, c -> {
                    List<Date> found = jdbc.queryForList("SELECT claim_date FROM claim WHERE sheet_id = ? AND person_id = ?"
                            + " AND claim_no = ? ORDER BY id LIMIT 1", Date.class, sheetId, c.personId(), c.corrects());
                    return found.isEmpty() ? null : found.get(0).toLocalDate().toString();
                }, added);
    }

    private static LocalDate local(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    private static String text(Date d) {
        return d == null ? null : d.toLocalDate().toString();
    }

    /** A from-date as a key: a null one is the beginning of time. */
    private static LocalDate from(Date d) {
        return d == null ? SheetAsOf.BEGINNING : d.toLocalDate();
    }
}
