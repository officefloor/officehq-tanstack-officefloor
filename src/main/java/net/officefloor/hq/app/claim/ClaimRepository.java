package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.settings.SettingsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, person_id, amount, claim_date, note, status, claim_no, settled_due, opened_by, stated, stated_due, corrects, due_by, stated_na, deferred_until, proofs, split_from,"
            + " (status = 'OPEN' AND deferred_until IS NOT NULL AND deferred_until > (SELECT s.today FROM sheet s WHERE s.id = claim.sheet_id)) AS deferred";

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person_id"),
            money(rs.getBigDecimal("amount")), rs.getDate("claim_date").toLocalDate().toString(),
            rs.getString("note"), rs.getString("status"),
            rs.getString("claim_no"), dueOf(rs.getBigDecimal("settled_due")),
            rs.getString("opened_by"), rs.getBoolean("stated"), dueOf(rs.getBigDecimal("stated_due")),
            rs.getString("corrects"), rs.getDate("due_by").toLocalDate().toString(),
            rs.getBoolean("stated_na"),
            rs.getDate("deferred_until") == null ? null : rs.getDate("deferred_until").toLocalDate().toString(),
            rs.getBoolean("deferred"), rs.getInt("proofs"), rs.getString("split_from"));

    /** The grade a person (sheet, person id) has on a date: the one with the latest from-date on
     *  or before it, the first one they were given applying from the beginning of time. */
    private static final String GRADE_ON = "(SELECT g.grade FROM person_grade g WHERE g.sheet_id = ? AND g.person_id = ?"
            + " AND (g.from_date IS NULL OR g.from_date <= CAST(? AS DATE)) ORDER BY g.from_date DESC NULLS LAST LIMIT 1)";

    /** The rate that would apply on a date (sheet, then GRADE_ON's person and date, then the date):
     *  the one for the person's grade with the latest from-date on or before it, an undated rate
     *  applying from the beginning of time; at most one row, with its pct and to_date. */
    private static final String RATE_ON = "(SELECT r.pct, r.to_date FROM rate r WHERE r.sheet_id = ? AND r.grade = "
            + GRADE_ON + " AND (r.from_date IS NULL OR r.from_date <= CAST(? AS DATE))"
            + " ORDER BY r.from_date DESC NULLS LAST LIMIT 1)";

    private final JdbcTemplate jdbc;
    private final SettingsRepository settings;

    public ClaimRepository(JdbcTemplate jdbc, SettingsRepository settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    public boolean personExists(String sheetId, long personId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ?",
                Integer.class, sheetId, personId);
        return known != null && known > 0;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY listed DESC, id DESC", ROW, sheetId);
    }

    /** A claim on the sheet; null when the sheet has no such claim. */
    public Claim find(String sheetId, long claimId) {
        List<Claim> found = jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND id = ?", ROW,
                sheetId, claimId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Move a claim to a status, only when it is now in one of {@code from}; false when it is not. */
    public boolean changeStatus(String sheetId, long claimId, List<String> from, String to) {
        String in = String.join(", ", from.stream().map(s -> "?").toList());
        List<Object> args = new ArrayList<>(List.of(to, claimId, sheetId));
        args.addAll(from);
        return jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND sheet_id = ? AND status IN (" + in + ")",
                args.toArray()) > 0;
    }

    /** Give an opened claim the sheet's next number. The sheet's row stays locked until the
     *  transaction commits, so two claims opened at once get consecutive numbers. */
    public String number(String sheetId, long claimId) {
        jdbc.update("UPDATE sheet SET claim_number = claim_number + 1 WHERE id = ?", sheetId);
        Long next = jdbc.queryForObject("SELECT claim_number FROM sheet WHERE id = ?", Long.class, sheetId);
        String number = String.valueOf(next);
        jdbc.update("UPDATE claim SET claim_no = ? WHERE id = ? AND sheet_id = ?", number, claimId, sheetId);
        return number;
    }

    /** Whether claims opened now are numbered per period: the sheet's period numbering from-date is set
     *  and its today is on or after it. */
    public boolean numbersPerPeriod(String sheetId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ? AND period_numbering_from IS NOT NULL"
                + " AND today >= period_numbering_from", Integer.class, sheetId);
        return n != null && n > 0;
    }

    /** Give an opened claim the next number in the sequence of the period its date falls in,
     *  <period name>-<n>, or X-<n> when it is in no period. The sheet's row is locked first, so two
     *  claims opened at once get consecutive numbers. */
    public String numberInPeriod(String sheetId, long claimId, LocalDate date) {
        jdbc.update("UPDATE sheet SET claim_number = claim_number WHERE id = ?", sheetId);
        List<Object[]> period = jdbc.query("SELECT id, name FROM period WHERE sheet_id = ? AND from_date <= ?"
                + " AND to_date >= ? ORDER BY from_date LIMIT 1",
                (rs, i) -> new Object[] { rs.getLong("id"), rs.getString("name") }, sheetId, Date.valueOf(date),
                Date.valueOf(date));
        String key = period.isEmpty() ? "X" : "P" + period.get(0)[0];
        String prefix = period.isEmpty() ? "X" : (String) period.get(0)[1];
        if (jdbc.update("UPDATE claim_sequence SET last_no = last_no + 1 WHERE sheet_id = ? AND seq_key = ?",
                sheetId, key) == 0) {
            jdbc.update("INSERT INTO claim_sequence (sheet_id, seq_key, last_no) VALUES (?, ?, 1)", sheetId, key);
        }
        Long next = jdbc.queryForObject("SELECT last_no FROM claim_sequence WHERE sheet_id = ? AND seq_key = ?",
                Long.class, sheetId, key);
        String number = prefix + "-" + next;
        jdbc.update("UPDATE claim SET claim_no = ? WHERE id = ? AND sheet_id = ?", number, claimId, sheetId);
        return number;
    }

    /** Record who opened a claim. */
    public void openedBy(String sheetId, long claimId, String by) {
        jdbc.update("UPDATE claim SET opened_by = ? WHERE id = ? AND sheet_id = ?", by, claimId, sheetId);
    }

    /** A claim's due now: its amount at the rate, in force on the claim's date, for the grade its person
     *  had on that date
     *  (the one with the latest from-date on or before it; an undated rate applies from the beginning
     *  of time; none applies when the claim's date is after that rate's to-date), rounded to two decimals by the claim's
     *  rounding (see {@link #rounding}) — the rate figure, not floored: the floor is applied last, in {@link #dueNow};
     *  null when no rate applies. */
    public BigDecimal due(String sheetId, Claim claim) {
        List<BigDecimal> pct = jdbc.queryForList("SELECT a.pct FROM " + RATE_ON + " a"
                + " WHERE a.to_date IS NULL OR a.to_date >= CAST(? AS DATE)", BigDecimal.class, sheetId,
                sheetId, claim.personId(), claim.date(), claim.date(), claim.date());
        if (pct.isEmpty()) return null;
        BigDecimal due = new BigDecimal(claim.amount()).multiply(pct.get(0)).movePointLeft(2)
                .setScale(2, rounding(sheetId, claim));
        return due;
    }

    /** How a claim's live figures round: half up when the claim is dated before the sheet's policy date
     *  (blank meaning the sheet's today), else half to even. A correction rounds by the date of the
     *  claim it corrects (its person's claim with that number), not its own. */
    public RoundingMode rounding(String sheetId, Claim claim) {
        String date = claim.date();
        if (claim.corrects() != null) {
            List<Date> corrected = jdbc.queryForList("SELECT claim_date FROM claim WHERE sheet_id = ? AND person_id = ?"
                    + " AND claim_no = ? ORDER BY id LIMIT 1", Date.class, sheetId, claim.personId(), claim.corrects());
            if (!corrected.isEmpty()) date = corrected.get(0).toLocalDate().toString();
        }
        List<Date> policy = jdbc.queryForList("SELECT COALESCE(policy_date, today) FROM sheet WHERE id = ?",
                Date.class, sheetId);
        if (policy.isEmpty()) return RoundingMode.HALF_EVEN;
        return LocalDate.parse(date).isBefore(policy.get(0).toLocalDate()) ? RoundingMode.HALF_UP
                : RoundingMode.HALF_EVEN;
    }

    /** A claim's figure now: the due fixed when it was settled, else the one its last statement bound
     *  it to (not while PENDING; none when it bound it to n/a), else its live rate figure (not floored); null when it has none. */
    public BigDecimal figure(String sheetId, Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        if (claim.bound() && claim.statedNa()) return null;
        if (claim.bound() && claim.statedDue() != null) return new BigDecimal(claim.statedDue());
        return due(sheetId, claim);
    }

    /** A claim's due now: a settled claim's frozen due; else the greater of the sheet's floor in force on the
     *  claim's date and its figure (the one its last statement bound it to, else its live rate figure) plus, while it
     *  is overdue (OPEN or PENDING and its due-by — fixed when it was created — before the sheet's today), for every
     *  full 30 days it is overdue, the sheet's late percent in force on that boundary date (the due-by plus 30, 60,
     *  90 ... days) of that figure, each rounded to two decimals by the claim's rounding, the charges added up (a
     *  boundary whose percent is 0.00 adds nothing). A statement never binds the charge: it keeps following the
     *  sheet's today until settling freezes it in. A correction (a negative amount) has no floor and no late charge,
     *  nor does a deferred claim have a charge (it is not overdue). Null when it has no figure. */
    public BigDecimal dueNow(String sheetId, Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        BigDecimal figure = figure(sheetId, claim);
        if (figure == null || new BigDecimal(claim.amount()).signum() < 0) return figure;
        BigDecimal due = figure.add(lateCharge(sheetId, claim, figure));
        return due.max(settings.floor(sheetId, LocalDate.parse(claim.date())));
    }

    /** The late charge on a claim's figure now (see {@link #dueNow}); zero when it is not overdue. */
    private BigDecimal lateCharge(String sheetId, Claim claim, BigDecimal figure) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (!claim.open() || claim.deferred()) return charge;
        List<Date> found = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId);
        if (found.isEmpty()) return charge;
        LocalDate today = found.get(0).toLocalDate();
        LocalDate dueBy = LocalDate.parse(claim.dueBy());
        long periods = dueBy.isBefore(today) ? ChronoUnit.DAYS.between(dueBy, today) / 30 : 0;
        if (periods == 0) return charge;
        RoundingMode rounding = rounding(sheetId, claim);
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = settings.latePct(sheetId, dueBy.plusDays(30 * n));
            if (pct == null || pct.signum() == 0) continue;
            charge = charge.add(figure.multiply(pct).movePointLeft(2).setScale(2, rounding));
        }
        return charge;
    }

    /** Whether the rate that would apply to a claim (for the grade its person had on its date, the one
     *  with the latest from-date on or before it) has ended before the claim's date. */
    public boolean rateEnded(String sheetId, Claim claim) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + RATE_ON + " a"
                + " WHERE a.to_date < CAST(? AS DATE)", Integer.class, sheetId,
                sheetId, claim.personId(), claim.date(), claim.date(), claim.date());
        return n != null && n > 0;
    }

    /** Whether a rate for the grade the person has on a date is in force on that date (not past its
     *  to-date). */
    public boolean rated(String sheetId, long personId, LocalDate on) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + RATE_ON + " a"
                + " WHERE a.to_date IS NULL OR a.to_date >= CAST(? AS DATE)", Integer.class, sheetId,
                sheetId, personId, on.toString(), on.toString(), on.toString());
        return n != null && n > 0;
    }

    /** Bind a claim to a figure (stated; to n/a when {@code na}), or unbind it (not stated, live again). */
    public void bind(String sheetId, long claimId, boolean stated, BigDecimal figure, boolean na) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ?, stated_na = ? WHERE id = ? AND sheet_id = ?",
                stated, figure, na,
                claimId, sheetId);
    }

    /** A person's claims on the sheet. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC, id DESC",
                ROW, sheetId, personId);
    }

    /** Move a DRAFT, OPEN or PENDING claim to another person: it is unbound from its last statement (live
     *  again) and listed as its new person's newest claim; false when the sheet has no such claim in those statuses. */
    public boolean reassign(String sheetId, long claimId, long personId) {
        return jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, stated_na = FALSE,"
                + " listed = NEXT VALUE FOR claim_listed_seq WHERE id = ? AND sheet_id = ?"
                + " AND status IN ('DRAFT', 'OPEN', 'PENDING')", personId, claimId, sheetId) > 0;
    }

    /** Defer an OPEN claim until a date; false when the sheet has no such OPEN claim. */
    public boolean defer(String sheetId, long claimId, LocalDate until) {
        return jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND sheet_id = ? AND status = 'OPEN'",
                Date.valueOf(until), claimId, sheetId) > 0;
    }

    /** Raise a claim's proofs count by one, only when it is not SETTLED or VOID; the count now, or null when the
     *  sheet has no such claim in those statuses. */
    public Integer addProof(String sheetId, long claimId) {
        if (jdbc.update("UPDATE claim SET proofs = proofs + 1 WHERE id = ? AND sheet_id = ?"
                + " AND status NOT IN ('SETTLED', 'VOID')", claimId, sheetId) == 0) {
            return null;
        }
        return jdbc.queryForObject("SELECT proofs FROM claim WHERE id = ? AND sheet_id = ?", Integer.class, claimId, sheetId);
    }

    /** Fix a settled claim's due. */
    public void settleDue(String sheetId, long claimId, BigDecimal due) {
        jdbc.update("UPDATE claim SET settled_due = ? WHERE id = ? AND sheet_id = ?", due, claimId, sheetId);
    }

    /** The name of a person on the sheet. */
    public String personName(String sheetId, long personId) {
        return jdbc.queryForObject("SELECT name FROM person WHERE sheet_id = ? AND id = ?",
                String.class, sheetId, personId);
    }

    /** Whether the person has a SETTLED claim with this number on the sheet. */
    public boolean settledOf(String sheetId, long personId, String number) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_no = ? AND status = 'SETTLED'", Integer.class, sheetId, personId, number);
        return n != null && n > 0;
    }

    /** The due-by of a claim created now for a person with a date: the date plus the person's own terms,
     *  else the terms, in force on it, of the grade the person has on it, else the sheet's terms in force on it. */
    public LocalDate dueBy(String sheetId, long personId, LocalDate date) {
        List<Integer> mine = jdbc.queryForList("SELECT terms FROM person WHERE sheet_id = ? AND id = ?", Integer.class,
                sheetId, personId);
        if (!mine.isEmpty() && mine.get(0) != null) {
            return date.plusDays(mine.get(0));
        }
        List<String> grade = jdbc.queryForList("SELECT " + GRADE_ON + " FROM DUAL", String.class, sheetId, personId,
                date.toString());
        Integer own = grade.isEmpty() || grade.get(0) == null ? null : settings.gradeTerms(sheetId, grade.get(0), date);
        return date.plusDays(own != null ? own : settings.terms(sheetId, date));
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects) {
        LocalDate dueBy = dueBy(sheetId, personId, date);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), personId, money(amount), date.toString(), note, "DRAFT", null, null, null, false, null, corrects,
                dueBy.toString(), false, null, false, 0, null);
    }

    /** Split an amount off an OPEN claim: the claim keeps the rest of its amount and is unbound from its last
     *  statement (live again); a new OPEN claim of the same person with the split-off amount, the same date and
     *  note, opened by {@code by}, with its due-by fixed now and the original's number as split-from, is added
     *  (unnumbered: the caller numbers it). The new claim's id, or null when the sheet has no such OPEN claim. */
    public Long split(String sheetId, Claim claim, BigDecimal amount, String by) {
        BigDecimal rest = new BigDecimal(claim.amount()).subtract(amount);
        if (jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL, stated_na = FALSE"
                + " WHERE id = ? AND sheet_id = ? AND status = 'OPEN'", rest, claim.id(), sheetId) == 0) {
            return null;
        }
        LocalDate date = LocalDate.parse(claim.date());
        LocalDate dueBy = dueBy(sheetId, claim.personId(), date);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, due_by, status, opened_by, split_from)"
                            + " VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, claim.note());
            ps.setDate(6, Date.valueOf(dueBy));
            ps.setString(7, by);
            ps.setString(8, claim.number());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Change a DRAFT claim's amount, date, note and the number it corrects; false when the sheet has no
     *  such draft claim. */
    public boolean update(String sheetId, long claimId, BigDecimal amount, LocalDate date, String note, String corrects) {
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE id = ?"
                + " AND sheet_id = ? AND status = 'DRAFT'",
                amount, Date.valueOf(date), note, corrects, claimId, sheetId) > 0;
    }

    /** Write a line of a claim's status log: its status now, who moved it there, the sheet's today and its
     *  due as computed now. */
    public void log(String sheetId, long claimId, String by) {
        Claim claim = find(sheetId, claimId);
        if (claim == null) return;
        jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due)"
                + " SELECT ?, ?, ?, ?, today, ? FROM sheet WHERE id = ?",
                sheetId, claimId, claim.status(), by, dueNow(sheetId, claim), sheetId);
    }

    /** How many days the sheet's today is past the date a claim's status log last records it SETTLED; null when
     *  it records no SETTLED line. */
    public Long daysSinceSettled(String sheetId, long claimId) {
        List<Long> found = jdbc.query("SELECT DATEDIFF('DAY', l.on_date, s.today) AS days FROM claim_log l"
                + " JOIN sheet s ON s.id = l.sheet_id WHERE l.sheet_id = ? AND l.claim_id = ? AND l.status = 'SETTLED'"
                + " ORDER BY l.id DESC LIMIT 1", (rs, i) -> rs.getLong("days"), sheetId, claimId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The status logs of the sheet's claims, by claim id: each a line per status it has had, oldest first,
     *  as "<STATUS> by <user> on <date> due=<due>" ('n/a' when it had no due). */
    public Map<String, List<String>> logs(String sheetId) {
        Map<String, List<String>> logs = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, status, by_user, on_date, due FROM claim_log WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    BigDecimal due = rs.getBigDecimal("due");
                    logs.computeIfAbsent(String.valueOf(rs.getLong("claim_id")), k -> new ArrayList<>())
                            .add(rs.getString("status") + " by " + rs.getString("by_user") + " on "
                                    + rs.getDate("on_date").toLocalDate() + " due=" + (due == null ? "n/a" : money(due)));
                }, sheetId);
        return logs;
    }

    /** Each claim's status at the end of a day, by claim id, as its status log records it: the last line
     *  dated on or before it. A claim with no such line (added after that day) is absent. */
    public Map<Long, String> statusesOn(String sheetId, LocalDate day) {
        Map<Long, String> statuses = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, status FROM claim_log WHERE sheet_id = ? AND on_date <= ? ORDER BY id",
                rs -> {
                    statuses.put(rs.getLong("claim_id"), rs.getString("status"));
                }, sheetId, Date.valueOf(day));
        return statuses;
    }

    /** An amount as shown: two decimals, a dot, no grouping. */
    static String money(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }

    /** A fixed due as shown, or null when there is none. */
    static String dueOf(BigDecimal due) {
        return due == null ? null : money(due);
    }
}
