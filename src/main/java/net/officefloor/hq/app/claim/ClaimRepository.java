package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.HistoryRepository;
import net.officefloor.hq.app.settings.SettingsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, person_id, amount, claim_date, note, status, claim_no, settled_due, opened_by, stated, stated_due, corrects, due_by, stated_na, deferred_until, proofs, split_from, late_entry, reminded, ref, tags, void_reason, waived, proofs_photo, held,"
            + " (status = 'OPEN' AND deferred_until IS NOT NULL AND deferred_until > (SELECT s.today FROM sheet s WHERE s.id = claim.sheet_id)) AS deferred";

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person_id"),
            money(rs.getBigDecimal("amount")), rs.getDate("claim_date").toLocalDate().toString(),
            rs.getString("note"), rs.getString("status"),
            rs.getString("claim_no"), dueOf(rs.getBigDecimal("settled_due")),
            rs.getString("opened_by"), rs.getBoolean("stated"), dueOf(rs.getBigDecimal("stated_due")),
            rs.getString("corrects"), rs.getDate("due_by").toLocalDate().toString(),
            rs.getBoolean("stated_na"),
            rs.getDate("deferred_until") == null ? null : rs.getDate("deferred_until").toLocalDate().toString(),
            rs.getBoolean("deferred"), rs.getInt("proofs"), rs.getString("split_from"),
            rs.getBoolean("late_entry"),
            rs.getDate("reminded") == null ? null : rs.getDate("reminded").toLocalDate().toString(),
            rs.getString("ref"), rs.getString("tags"), rs.getString("void_reason"), rs.getBoolean("waived"), rs.getInt("proofs_photo"), rs.getBoolean("held"));

    /** The grade a person (sheet, person id) has on a date: the one with the latest from-date on
     *  or before it, the first one they were given applying from the beginning of time. */
    private static final String GRADE_ON = "(SELECT g.grade FROM person_grade g WHERE g.sheet_id = ? AND g.person_id = ?"
            + " AND (g.from_date IS NULL OR g.from_date <= CAST(? AS DATE)) ORDER BY g.from_date DESC NULLS LAST LIMIT 1)";

    /** The rate a sheet has for a grade on a date (sheet, grade, then the date): the one with the latest
     *  from-date on or before it, an undated rate applying from the beginning of time; at most one row, with
     *  its pct and to_date. */
    private static final String RATE_FOR = "(SELECT r.pct, r.to_date FROM rate r WHERE r.sheet_id = ? AND r.grade = ?"
            + " AND (r.from_date IS NULL OR r.from_date <= CAST(? AS DATE)) ORDER BY r.from_date DESC NULLS LAST LIMIT 1)";

    /** The rate a sheet's claim takes for a grade on a date (sheet, grade, the date, then the claim's added number):
     *  as {@link #RATE_FOR}, among only the rate versions that apply to the claim — those that apply to every claim
     *  and the forward-only ones saved before it was added — the newest version of each from-date; at most one row,
     *  with its pct and to_date. */
    private static final String CLAIM_RATE_FOR = "(SELECT r.pct, r.to_date FROM (SELECT sheet_id, grade, from_date, version,"
            + " pct, to_date, after_seq FROM rate UNION ALL SELECT sheet_id, grade, from_date, version, pct, to_date, after_seq"
            + " FROM rate_version) r WHERE r.sheet_id = ? AND r.grade = ?"
            + " AND (r.from_date IS NULL OR r.from_date <= CAST(? AS DATE)) AND (r.after_seq IS NULL OR r.after_seq < ?)"
            + " ORDER BY r.from_date DESC NULLS LAST, r.version DESC LIMIT 1)";

    private final JdbcTemplate jdbc;
    private final SettingsRepository settings;
    private final HistoryRepository history;

    public ClaimRepository(JdbcTemplate jdbc, SettingsRepository settings, HistoryRepository history) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.history = history;
    }

    /** Keep a claim's values in the sheet's history after a change to it; whether it changed. */
    private boolean kept(String sheetId, long claimId, boolean changed) {
        if (changed) history.claim(sheetId, claimId);
        return changed;
    }

    public boolean personExists(String sheetId, long personId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ?",
                Integer.class, sheetId, personId);
        return known != null && known > 0;
    }

    /** The given claims as the sheet lists them: people in list order (by their position), each person's claims
     *  newest first (a reassigned claim counts as its new person's newest); ids the sheet does not have are left out. */
    public List<Claim> inListOrder(String sheetId, Collection<Long> ids) {
        List<Claim> out = new ArrayList<>();
        for (Claim c : list(sheetId)) {
            if (ids.contains(c.id())) out.add(c);
        }
        Map<Long, Integer> place = new java.util.HashMap<>();
        for (Long pid : jdbc.queryForList("SELECT id FROM person WHERE sheet_id = ? ORDER BY list_position, id",
                Long.class, sheetId)) {
            place.put(pid, place.size());
        }
        out.sort(Comparator.comparingInt((Claim c) -> place.getOrDefault(c.personId(), Integer.MAX_VALUE)));
        return out;
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
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET status = ? WHERE id = ? AND sheet_id = ? AND status IN (" + in + ")",
                args.toArray()) > 0);
    }

    /** Record the sheet's today as the claim's review day (it has just gone to REVIEW). */
    public void reviewDay(String sheetId, long claimId) {
        kept(sheetId, claimId, jdbc.update("UPDATE claim SET review_day = ? WHERE id = ? AND sheet_id = ?",
                java.sql.Date.valueOf(settings.today(sheetId)), claimId, sheetId) > 0);
    }

    /** Whether each REVIEW claim of the sheet is review-late (its review day plus the sheet's review SLA is before the
     *  sheet's today; none is while the SLA is blank), by claim id. */
    public Map<String, Boolean> reviewLate(String sheetId) {
        Map<String, Boolean> late = new LinkedHashMap<>();
        Integer sla = settings.reviewSla(sheetId);
        if (sla == null) return late;
        LocalDate today = settings.today(sheetId);
        jdbc.query("SELECT id, review_day FROM claim WHERE sheet_id = ? AND status = 'REVIEW' AND review_day IS NOT NULL",
                rs -> {
                    late.put(String.valueOf(rs.getLong("id")),
                            rs.getDate("review_day").toLocalDate().plusDays(sla).isBefore(today));
                }, sheetId);
        return late;
    }

    /** Record the reason a claim was voided with. */
    public void voidReason(String sheetId, long claimId, String reason) {
        kept(sheetId, claimId, jdbc.update("UPDATE claim SET void_reason = ? WHERE id = ? AND sheet_id = ?", reason, claimId, sheetId) > 0);
    }

    /** Give an opened claim the next number of its sheet's tree within the fiscal year its due-by ({@code date})
     *  falls in, FY<yyyy>-<n>: one after the last number given in that fiscal year on any sheet of the tree (the
     *  sheet's topmost ancestor and all its descendants). The fiscal year follows the sheet's fiscal start month.
     *  The tree's sheet rows stay locked until the transaction commits, so two claims opened at once anywhere in
     *  the tree get consecutive numbers. */
    public String number(String sheetId, long claimId, LocalDate date) {
        List<String> tree = tree(sheetId);
        String in = String.join(",", java.util.Collections.nCopies(tree.size(), "?"));
        jdbc.update("UPDATE sheet SET claim_number = claim_number WHERE id IN (" + in + ")", tree.toArray());
        Integer startMonth = jdbc.queryForObject("SELECT fiscal_start_month FROM sheet WHERE id = ?", Integer.class, sheetId);
        int year = date.getMonthValue() >= (startMonth == null ? 1 : startMonth) ? date.getYear() : date.getYear() - 1;
        String key = "FY" + year;
        List<Object> args = new ArrayList<>(tree);
        args.add(key);
        Long last = jdbc.queryForObject("SELECT MAX(last_no) FROM claim_sequence WHERE sheet_id IN (" + in + ")"
                + " AND seq_key = ?", Long.class, args.toArray());
        long next = (last == null ? 0 : last) + 1;
        if (jdbc.update("UPDATE claim_sequence SET last_no = ? WHERE sheet_id = ? AND seq_key = ?", next, sheetId, key) == 0) {
            jdbc.update("INSERT INTO claim_sequence (sheet_id, seq_key, last_no) VALUES (?, ?, ?)", sheetId, key, next);
        }
        String number = key + "-" + next;
        jdbc.update("UPDATE claim SET claim_no = ? WHERE id = ? AND sheet_id = ?", number, claimId, sheetId);
        kept(sheetId, claimId, true);
        return number;
    }

    /** The sheets of the sheet's tree, sorted: its topmost ancestor (following parents) and all that one's
     *  descendants, the sheet itself always among them. */
    private List<String> tree(String sheetId) {
        String root = sheetId;
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        for (String at = parentOf(root); at != null && seen.add(at); at = parentOf(at)) root = at;
        java.util.Set<String> found = new java.util.TreeSet<>(List.of(root));
        java.util.Deque<String> pending = new java.util.ArrayDeque<>(List.of(root));
        while (!pending.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT id FROM sheet WHERE parent_id = ?", String.class, pending.poll())) {
                if (found.add(child)) pending.add(child);
            }
        }
        found.add(sheetId);
        return new ArrayList<>(found);
    }

    private String parentOf(String sheetId) {
        List<String> found = jdbc.queryForList("SELECT parent_id FROM sheet WHERE id = ?", String.class, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether claims opened now are numbered per period: the sheet's period numbering from-date is set
     *  and its today is on or after it. */
    public boolean numbersPerPeriod(String sheetId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ? AND period_numbering_from IS NOT NULL"
                + " AND today >= period_numbering_from", Integer.class, sheetId);
        return n != null && n > 0;
    }

    /** Give an opened claim the next number in the sequence of the period its due-by ({@code date}) falls in,
     *  <period name>-<n>, or X-<n> when it is in no period. The sheet's row is locked first, so two
     *  claims opened at once get consecutive numbers. */
    public String numberInPeriod(String sheetId, long claimId, LocalDate date) {
        jdbc.update("UPDATE sheet SET claim_number = claim_number WHERE id = ?", sheetId);
        List<Object[]> period = periodOn(sheetId, date);
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
        kept(sheetId, claimId, true);
        return number;
    }

    /** The period a date falls in, as [id, name] (the earliest by from-date), or none. */
    private List<Object[]> periodOn(String sheetId, LocalDate date) {
        return jdbc.query("SELECT id, name FROM period WHERE sheet_id = ? AND from_date <= ?"
                + " AND to_date >= ? ORDER BY from_date LIMIT 1",
                (rs, i) -> new Object[] { rs.getLong("id"), rs.getString("name") }, sheetId, Date.valueOf(date),
                Date.valueOf(date));
    }

    /** Re-date an OPEN or PENDING claim (its due-by and statement binding stay as they are); false when the
     *  sheet has no such claim in those statuses. */
    public boolean redate(String sheetId, long claimId, LocalDate date) {
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET claim_date = ? WHERE id = ? AND sheet_id = ? AND status IN ('OPEN', 'PENDING')",
                Date.valueOf(date), claimId, sheetId) > 0);
    }

    /** Record who last edited a claim. */
    public void editedBy(String sheetId, long claimId, String by) {
        jdbc.update("UPDATE claim SET edited_by = ? WHERE id = ? AND sheet_id = ?", by, claimId, sheetId);
    }

    /** Who last edited a claim; null when it was never edited. */
    public String editedBy(String sheetId, long claimId) {
        List<String> by = jdbc.queryForList("SELECT edited_by FROM claim WHERE id = ? AND sheet_id = ?", String.class, claimId, sheetId);
        return by.isEmpty() ? null : by.get(0);
    }

    /** Record who opened a claim. */
    public void openedBy(String sheetId, long claimId, String by) {
        jdbc.update("UPDATE claim SET opened_by = ? WHERE id = ? AND sheet_id = ?", by, claimId, sheetId);
    }

    /** A claim's due now: its amount at the rate, in force on the claim's date, for the grade its person
     *  had on that date, falling back to the parent sheets' (see {@link #rateInForce})
     *  (the one with the latest from-date on or before it; an undated rate applies from the beginning
     *  of time; none applies when the claim's date is after that rate's to-date), rounded to two decimals by the claim's
     *  rounding (see {@link #rounding}) — the rate figure, not floored: the floor is applied before the late charge, in {@link #dueNow};
     *  null when no rate applies. */
    public BigDecimal due(String sheetId, Claim claim) {
        BigDecimal pct = rateInForce(sheetId, claim.personId(), LocalDate.parse(claim.date()), added(sheetId, claim.id()));
        if (pct == null) return null;
        BigDecimal due = new BigDecimal(claim.amount()).multiply(pct).movePointLeft(2)
                .setScale(2, rounding(sheetId, claim));
        return due;
    }

    /** How a claim's live figures round: by the sheet's rounding mode in force on the claim's date (half-up or
     *  half-even) when there is one; else half up when the claim is dated before the sheet's policy date
     *  (blank meaning the sheet's today), else half to even. A correction rounds by the date of the
     *  claim it corrects (its person's claim with that number), not its own. */
    public RoundingMode rounding(String sheetId, Claim claim) {
        String date = claim.date();
        if (claim.corrects() != null) {
            List<Date> corrected = jdbc.queryForList("SELECT claim_date FROM claim WHERE sheet_id = ? AND person_id = ?"
                    + " AND claim_no = ? ORDER BY id LIMIT 1", Date.class, sheetId, claim.personId(), claim.corrects());
            if (!corrected.isEmpty()) date = corrected.get(0).toLocalDate().toString();
        }
        List<String> mode = jdbc.queryForList("SELECT setting FROM setting_value WHERE sheet_id = ? AND setting_key = 'rounding'"
                + " AND from_date <= ? ORDER BY from_date DESC LIMIT 1", String.class, sheetId, Date.valueOf(LocalDate.parse(date)));
        RoundingMode set = mode.isEmpty() ? null : SettingsRepository.roundingMode(mode.get(0));
        if (set != null) return set;
        List<Date> policy = jdbc.queryForList("SELECT COALESCE(policy_date, today) FROM sheet WHERE id = ?",
                Date.class, sheetId);
        if (policy.isEmpty()) return RoundingMode.HALF_EVEN;
        return LocalDate.parse(date).isBefore(policy.get(0).toLocalDate()) ? RoundingMode.HALF_UP
                : RoundingMode.HALF_EVEN;
    }

    /** A claim's figure now: the due fixed when it was settled, else the due as shown its last statement bound
     *  it to (floor and late charge included; none when it bound it to n/a), else its live rate figure (not floored);
     *  null when it has none. */
    public BigDecimal figure(String sheetId, Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        if (claim.bound() && claim.statedNa()) return null;
        if (claim.bound() && claim.statedDue() != null) return new BigDecimal(claim.statedDue());
        return due(sheetId, claim);
    }

    /** A claim's due now: a settled claim's frozen due; else its floored figure (the greater of the sheet's floor in
     *  force on the claim's date and its live rate figure) plus, while it
     *  is overdue (OPEN or PENDING and its due-by — fixed when it was created — plus its grade's grace days before the
     *  sheet's today), for every full 30 days it is overdue, the sheet's late percent in force on that boundary date (the
     *  due-by plus the grace plus 30, 60,
     *  90 ... days) of that figure plus the charges of the boundaries before it (compounding), each rounded to two decimals by the claim's rounding, the charges added up (a
     *  boundary whose percent is 0.00 adds nothing). A claim bound by a statement shows the due its last statement
     *  bound it to, as it was shown then (late charge and floor included): its charge no longer follows the sheet's
     *  today until the statement is cancelled. A correction (a negative amount) has no floor and no late charge,
     *  nor does a deferred claim have a charge (it is not overdue). Null when it has no figure. */
    public BigDecimal dueNow(String sheetId, Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        if (claim.bound() && (claim.statedNa() || claim.statedDue() != null)) return figure(sheetId, claim);
        BigDecimal figure = figure(sheetId, claim);
        if (figure == null || new BigDecimal(claim.amount()).signum() < 0) return figure;
        BigDecimal floored = figure.max(settings.floor(sheetId, LocalDate.parse(claim.date())));
        return floored.add(lateCharge(sheetId, claim, floored));
    }

    /** The day a claim's overdue days are counted from: its due-by plus the grace days, in force on the claim's date,
     *  of the grade its person had on that date (its due-by when that grade has no grace then). The claim is overdue
     *  while open, not deferred, and this day is before the sheet's today; its 30-day late charge boundaries count
     *  from it. */
    public LocalDate overdueFrom(String sheetId, Claim claim) {
        LocalDate dueBy = LocalDate.parse(claim.dueBy());
        String grade = gradeOn(sheetId, claim);
        if (grade == null) return dueBy;
        Integer grace = settings.gradeGrace(sheetId, grade, LocalDate.parse(claim.date()));
        return grace == null ? dueBy : dueBy.plusDays(grace);
    }

    /** The grade a claim's person had on the claim's date; null when none. */
    public String gradeOn(String sheetId, Claim claim) {
        List<String> grade = jdbc.queryForList("SELECT " + GRADE_ON + " FROM DUAL", String.class, sheetId,
                claim.personId(), claim.date());
        return grade.isEmpty() ? null : grade.get(0);
    }

    /** The grade a person had on a day; null when none. */
    public String gradeOn(String sheetId, long personId, LocalDate day) {
        List<String> grade = jdbc.queryForList("SELECT " + GRADE_ON + " FROM DUAL", String.class, sheetId,
                personId, day.toString());
        return grade.isEmpty() ? null : grade.get(0);
    }

    /** Whether a claim is overdue on a day: open, not deferred, and its overdue-from day (see {@link #overdueFrom})
     *  before that day. */
    public boolean overdue(String sheetId, Claim claim, LocalDate today) {
        return claim.open() && !claim.deferred() && overdueFrom(sheetId, claim).isBefore(today);
    }

    /** The late charge on a claim's figure now (see {@link #dueNow}); zero when it is not overdue. */
    private BigDecimal lateCharge(String sheetId, Claim claim, BigDecimal figure) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (!claim.open() || claim.deferred() || claim.waived()) return charge;
        List<Date> found = jdbc.queryForList("SELECT today FROM sheet WHERE id = ?", Date.class, sheetId);
        if (found.isEmpty()) return charge;
        LocalDate today = found.get(0).toLocalDate();
        LocalDate dueBy = overdueFrom(sheetId, claim);
        long periods = dueBy.isBefore(today) ? ChronoUnit.DAYS.between(dueBy, today) / 30 : 0;
        if (periods == 0) return charge;
        RoundingMode rounding = rounding(sheetId, claim);
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = settings.latePct(sheetId, dueBy.plusDays(30 * n));
            if (pct == null || pct.signum() == 0) continue;
            charge = charge.add(figure.add(charge).multiply(pct).movePointLeft(2).setScale(2, rounding));
        }
        return charge;
    }

    /** Whether the rate that would apply to a claim (for the grade its person had on its date, the one
     *  with the latest from-date on or before it) has ended before the claim's date. */
    public boolean rateEnded(String sheetId, Claim claim) {
        String grade = gradeOn(sheetId, claim);
        if (grade == null) return false;
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + CLAIM_RATE_FOR + " a"
                + " WHERE a.to_date < CAST(? AS DATE)", Integer.class, sheetId, grade, claim.date(),
                added(sheetId, claim.id()), claim.date());
        return n != null && n > 0;
    }

    /** A claim's added number (see V83__forward_only_rate.sql): claims added later have higher ones; the highest
     *  there is for a claim the sheet does not have (yet). */
    public long added(String sheetId, long claimId) {
        List<Long> found = jdbc.queryForList("SELECT added_seq FROM claim WHERE sheet_id = ? AND id = ?", Long.class,
                sheetId, claimId);
        return found.isEmpty() || found.get(0) == null ? Long.MAX_VALUE : found.get(0);
    }

    /** Each claim's added number on the sheet, by claim id. */
    public Map<String, Long> added(String sheetId) {
        Map<String, Long> added = new LinkedHashMap<>();
        jdbc.query("SELECT id, added_seq FROM claim WHERE sheet_id = ?",
                rs -> {
                    added.put(String.valueOf(rs.getLong("id")), rs.getLong("added_seq"));
                }, sheetId);
        return added;
    }

    /** Whether a rate for the grade the person has on a date is in force on that date (not past its
     *  to-date). */
    public boolean rated(String sheetId, long personId, LocalDate on) {
        return rateInForce(sheetId, personId, on) != null;
    }

    /** The rate (a percent) in force on a date for the grade the person has on that date: this sheet's (see
     *  {@link #RATE_FOR}) when it has one in force then (not past its to-date), else its parent sheet's for that
     *  grade and date, then the parent's parent's, up to three levels up; null when none is. */
    public BigDecimal rateInForce(String sheetId, long personId, LocalDate on) {
        return rateInForce(sheetId, personId, on, null);
    }

    /** The rate (a percent) a claim takes: as {@link #rateInForce(String, long, LocalDate)} on its date, among only
     *  the rates that apply to it (see {@link #CLAIM_RATE_FOR}; {@code added} is its added number, null for all). */
    public BigDecimal rateInForce(String sheetId, long personId, LocalDate on, Long added) {
        List<String> grade = jdbc.queryForList("SELECT " + GRADE_ON + " FROM DUAL", String.class, sheetId,
                personId, on.toString());
        if (grade.isEmpty() || grade.get(0) == null) return null;
        List<String> chain = new ArrayList<>();
        chain.add(sheetId);
        chain.addAll(settings.rateAncestors(sheetId));
        for (String at : chain) {
            List<BigDecimal> pct = added == null
                    ? jdbc.queryForList("SELECT a.pct FROM " + RATE_FOR + " a"
                            + " WHERE a.to_date IS NULL OR a.to_date >= CAST(? AS DATE)", BigDecimal.class,
                            at, grade.get(0), on.toString(), on.toString())
                    : jdbc.queryForList("SELECT a.pct FROM " + CLAIM_RATE_FOR + " a"
                            + " WHERE a.to_date IS NULL OR a.to_date >= CAST(? AS DATE)", BigDecimal.class,
                            at, grade.get(0), on.toString(), added, on.toString());
            if (!pct.isEmpty()) return pct.get(0);
        }
        return null;
    }

    /** Bind a claim to a figure (stated; to n/a when {@code na}), or unbind it (not stated, live again). */
    public void bind(String sheetId, long claimId, boolean stated, BigDecimal figure, boolean na) {
        kept(sheetId, claimId, jdbc.update("UPDATE claim SET stated = ?, stated_due = ?, stated_na = ? WHERE id = ? AND sheet_id = ?",
                stated, figure, na,
                claimId, sheetId) > 0);
    }

    /** A person's claims on the sheet. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed DESC, id DESC",
                ROW, sheetId, personId);
    }

    /** Move a DRAFT, OPEN or PENDING claim to another person: it is unbound from its last statement (live
     *  again) and listed as its new person's newest claim; false when the sheet has no such claim in those statuses. */
    public boolean reassign(String sheetId, long claimId, long personId) {
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL, stated_na = FALSE,"
                + " listed = NEXT VALUE FOR claim_listed_seq WHERE id = ? AND sheet_id = ?"
                + " AND status IN ('DRAFT', 'OPEN', 'PENDING')", personId, claimId, sheetId) > 0);
    }

    /** Move every claim of one person to another (a merge): each keeps its number, its date and any figure a
     *  statement bound it to, and is listed after the other's claims in the order it was added. */
    public void moveAll(String sheetId, long fromId, long intoId) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                Long.class, sheetId, fromId);
        for (Long claimId : ids) {
            kept(sheetId, claimId, jdbc.update("UPDATE claim SET person_id = ?,"
                    + " listed = NEXT VALUE FOR claim_listed_seq WHERE id = ? AND sheet_id = ?",
                    intoId, claimId, sheetId) > 0);
        }
    }

    /** Move every claim of a person to another sheet, as the claims of the person {@code toPersonId} there: each keeps
     *  its status, dates, note, tags, settled due, status log, comments, lines and any figure a statement bound it to, and is listed
     *  in the same order; it loses its external reference, its lock and its settle batch. Each claim that had
     *  a number takes the other sheet's next one (as opening it there would give it), in the order the claims were
     *  opened (by the sheet's today when each was opened, then by the order they were added); a number one of them
     *  corrects or was split from follows it. The claims leave this sheet. */
    public void transferAll(String fromSheet, long personId, String toSheet, long toPersonId) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY listed, id",
                Long.class, fromSheet, personId);
        Map<Long, Long> moved = new LinkedHashMap<>();
        for (Long claimId : ids) {
            GeneratedKeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(c -> {
                PreparedStatement ps = c.prepareStatement("INSERT INTO claim (sheet_id, person_id, amount, claim_date, note,"
                        + " status, settled_due, opened_by, corrects, due_by, deferred_until, proofs, split_from, late_entry,"
                        + " reminded, tags, void_reason, edited_by, stated, stated_due, stated_na, waived, proofs_photo, held) SELECT ?, ?, amount, claim_date, note,"
                        + " status, settled_due, opened_by, corrects, due_by, deferred_until, proofs, split_from, late_entry,"
                        + " reminded, tags, void_reason, edited_by, stated, stated_due, stated_na, waived, proofs_photo, held FROM claim WHERE id = ? AND sheet_id = ?", new String[] { "id" });
                ps.setString(1, toSheet);
                ps.setLong(2, toPersonId);
                ps.setLong(3, claimId);
                ps.setString(4, fromSheet);
                return ps;
            }, keys);
            long arrived = keys.getKey().longValue();
            moved.put(claimId, arrived);
            jdbc.update("INSERT INTO claim_log (sheet_id, claim_id, status, by_user, on_date, due) SELECT ?, ?, status,"
                    + " by_user, on_date, due FROM claim_log WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                    toSheet, arrived, fromSheet, claimId);
            jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text) SELECT ?, ?, by_user,"
                    + " on_date, text FROM claim_comment WHERE sheet_id = ? AND claim_id = ? ORDER BY id",
                    toSheet, arrived, fromSheet, claimId);
            jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) SELECT ?, ?, label, qty, unit"
                    + " FROM claim_line WHERE sheet_id = ? AND claim_id = ? ORDER BY id", toSheet, arrived, fromSheet, claimId);
            used(toSheet, jdbc.queryForObject("SELECT tags FROM claim WHERE id = ?", String.class, arrived));
        }

        // Renumber on the other sheet, in the order the claims were opened.
        List<Object[]> numbered = jdbc.query("SELECT c.id, c.claim_no, c.due_by FROM claim c WHERE c.sheet_id = ?"
                + " AND c.person_id = ? AND c.claim_no IS NOT NULL ORDER BY (SELECT MIN(l.on_date) FROM claim_log l"
                + " WHERE l.sheet_id = c.sheet_id AND l.claim_id = c.id AND l.status = 'OPEN') NULLS LAST, c.id",
                (rs, i) -> new Object[] { rs.getLong("id"), rs.getString("claim_no"), rs.getDate("due_by").toLocalDate() },
                fromSheet, personId);
        boolean perPeriod = numbersPerPeriod(toSheet);
        Map<String, String> numbers = new LinkedHashMap<>();
        for (Object[] c : numbered) {
            long arrived = moved.get((Long) c[0]);
            numbers.put((String) c[1], perPeriod ? numberInPeriod(toSheet, arrived, (LocalDate) c[2]) : number(toSheet, arrived, (LocalDate) c[2]));
        }
        for (Long arrived : moved.values()) {
            Map<String, Object> was = jdbc.queryForMap("SELECT corrects, split_from FROM claim WHERE id = ?", arrived);
            String corrects = (String) was.get("corrects");
            String splitFrom = (String) was.get("split_from");
            jdbc.update("UPDATE claim SET corrects = ?, split_from = ? WHERE id = ?",
                    corrects == null ? null : numbers.getOrDefault(corrects, corrects),
                    splitFrom == null ? null : numbers.getOrDefault(splitFrom, splitFrom), arrived);
            history.claim(toSheet, arrived);
        }

        for (Long claimId : ids) {
            jdbc.update("DELETE FROM claim_log WHERE sheet_id = ? AND claim_id = ?", fromSheet, claimId);
            jdbc.update("DELETE FROM claim_comment WHERE sheet_id = ? AND claim_id = ?", fromSheet, claimId);
            jdbc.update("DELETE FROM claim_line WHERE sheet_id = ? AND claim_id = ?", fromSheet, claimId);
            jdbc.update("DELETE FROM claim WHERE id = ? AND sheet_id = ?", claimId, fromSheet);
        }
    }

    /** Defer an OPEN claim until a date; false when the sheet has no such OPEN claim. */
    public boolean defer(String sheetId, long claimId, LocalDate until) {
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND sheet_id = ? AND status = 'OPEN'",
                Date.valueOf(until), claimId, sheetId) > 0);
    }

    /** Raise a claim's proofs count by one (and its photos count too, for a photo), only when it is not SETTLED, VOID or DELETED; the count now, or null when the
     *  sheet has no such claim in those statuses. */
    public Integer addProof(String sheetId, long claimId, boolean photo) {
        if (jdbc.update("UPDATE claim SET proofs = proofs + 1" + (photo ? ", proofs_photo = proofs_photo + 1" : "")
                + " WHERE id = ? AND sheet_id = ?"
                + " AND status NOT IN ('SETTLED', 'VOID', 'DELETED')", claimId, sheetId) == 0) {
            return null;
        }
        return jdbc.queryForObject("SELECT proofs FROM claim WHERE id = ? AND sheet_id = ?", Integer.class, claimId, sheetId);
    }

    /** Waive an OPEN or PENDING claim's late charge, only when it is not already waived; false when the sheet has no such claim. */
    public boolean waive(String sheetId, long claimId) {
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET waived = TRUE WHERE id = ? AND sheet_id = ?"
                + " AND status IN ('OPEN', 'PENDING') AND NOT waived", claimId, sheetId) > 0);
    }

    /** Hold or release an OPEN or PENDING claim, only when it is not already so; false when the sheet has no such claim. */
    public boolean hold(String sheetId, long claimId, boolean held) {
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET held = ? WHERE id = ? AND sheet_id = ?"
                + " AND status IN ('OPEN', 'PENDING') AND held <> ?", held, claimId, sheetId, held) > 0);
    }

    /** Remind an OPEN or PENDING claim on the sheet's today, only when it was not already reminded that day;
     *  false when the sheet has no such claim. */
    public boolean remind(String sheetId, long claimId) {
        return jdbc.update("UPDATE claim c SET reminded = (SELECT s.today FROM sheet s WHERE s.id = c.sheet_id)"
                + " WHERE c.id = ? AND c.sheet_id = ? AND c.status IN ('OPEN', 'PENDING')"
                + " AND (c.reminded IS NULL OR c.reminded <> (SELECT s.today FROM sheet s WHERE s.id = c.sheet_id))",
                claimId, sheetId) > 0;
    }

    /** Escalate a claim reminded on the sheet's today when the sheet has an escalation gap, it was reminded before
     *  ({@code previous}) at least that many days before today, and it was never escalated; true when escalated. */
    public boolean escalate(String sheetId, long claimId, String previous) {
        Integer gap = settings.escalateAfter(sheetId);
        if (gap == null || previous == null) return false;
        if (LocalDate.parse(previous).plusDays(gap).isAfter(settings.today(sheetId))) return false;
        return jdbc.update("UPDATE claim SET escalated = TRUE WHERE id = ? AND sheet_id = ? AND NOT escalated",
                claimId, sheetId) > 0;
    }

    /** Fix a settled claim's due. */
    public void settleDue(String sheetId, long claimId, BigDecimal due) {
        kept(sheetId, claimId, jdbc.update("UPDATE claim SET settled_due = ? WHERE id = ? AND sheet_id = ?", due, claimId, sheetId) > 0);
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

    /** What remains of the person's SETTLED claim with this number: its amount plus the (negative) amounts of the
     *  corrections made against it that are not VOID or DELETED, leaving out {@code exceptId} (null: none). */
    public BigDecimal remainingOf(String sheetId, long personId, String number, Long exceptId) {
        BigDecimal target = jdbc.queryForObject("SELECT amount FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_no = ? AND status = 'SETTLED' ORDER BY id LIMIT 1", BigDecimal.class, sheetId, personId, number);
        BigDecimal made = jdbc.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND corrects = ? AND status NOT IN ('VOID', 'DELETED') AND id <> ?", BigDecimal.class,
                sheetId, personId, number, exceptId == null ? -1L : exceptId);
        return target.add(made).setScale(2);
    }

    /** The due-by of a claim created now for a person with a date: the terms-th business day after the date (skipping
     *  Saturdays, Sundays and the sheet's holidays), for the terms in force on the day it is created (the sheet's today):
     *  the person's own terms in force that day, else the terms, in force that day, of the grade the person has that day,
     *  else the sheet's terms in force that day. */
    public LocalDate dueBy(String sheetId, long personId, LocalDate date) {
        LocalDate on = settings.today(sheetId);
        // the person's own terms in force that day: the latest dated ones on or before it, else those from the beginning
        List<Integer> mine = new java.util.ArrayList<>();
        jdbc.query("SELECT terms FROM person_terms WHERE sheet_id = ? AND person_id = ? AND from_date <= ?"
                + " ORDER BY from_date DESC LIMIT 1", rs -> {
                    mine.add((Integer) rs.getObject("terms", Integer.class));
                }, sheetId, personId, Date.valueOf(on));
        if (mine.isEmpty()) {
            mine.addAll(jdbc.queryForList("SELECT terms FROM person WHERE sheet_id = ? AND id = ?", Integer.class,
                    sheetId, personId));
        }
        if (!mine.isEmpty() && mine.get(0) != null) {
            return settings.businessDaysAfter(sheetId, date, mine.get(0));
        }
        List<String> grade = jdbc.queryForList("SELECT " + GRADE_ON + " FROM DUAL", String.class, sheetId, personId,
                on.toString());
        return dueByOfGrade(sheetId, grade.isEmpty() ? null : grade.get(0), date);
    }

    /** The due-by of a claim created now with a date for a person of a grade (null when none) with no terms of their
     *  own: the terms-th business day after the date (skipping Saturdays, Sundays and the sheet's holidays), for the
     *  grade's terms in force on the day it is created (the sheet's today), else the sheet's terms in force that day. */
    public LocalDate dueByOfGrade(String sheetId, String grade, LocalDate date) {
        LocalDate on = settings.today(sheetId);
        Integer own = grade == null ? null : settings.gradeTerms(sheetId, grade, on);
        return settings.businessDaysAfter(sheetId, date, own != null ? own : settings.terms(sheetId, on));
    }

    /** Re-fix, after a holiday is added to the sheet, the due-by of each claim not SETTLED or VOID whose due-by is on
     *  or after it: the terms-th business day after its date over the sheet's holidays now, for the terms it was fixed
     *  with (the business days from its date to its due-by over the holidays before, {@code before}). A claim whose
     *  due-by moves is kept in the history. */
    public void refixDueBy(String sheetId, java.util.Set<String> before, LocalDate holiday) {
        java.util.Set<String> now = new java.util.HashSet<>(before);
        now.add(holiday.toString());
        List<Object[]> open = jdbc.query("SELECT id, claim_date, due_by FROM claim WHERE sheet_id = ?"
                + " AND status NOT IN ('SETTLED', 'VOID') AND due_by >= ? ORDER BY id",
                (rs, i) -> new Object[] { rs.getLong("id"), rs.getDate("claim_date").toLocalDate(),
                        rs.getDate("due_by").toLocalDate() }, sheetId, Date.valueOf(holiday));
        for (Object[] c : open) {
            LocalDate date = (LocalDate) c[1];
            LocalDate old = (LocalDate) c[2];
            LocalDate fixed = SettingsRepository.businessDaysAfter(now, date, businessDaysBetween(before, date, old));
            if (fixed.equals(old)) continue;
            jdbc.update("UPDATE claim SET due_by = ? WHERE id = ? AND sheet_id = ?", Date.valueOf(fixed), c[0], sheetId);
            history.claim(sheetId, (Long) c[0]);
        }
    }

    /** The business days (not a Saturday, Sunday or one of the holidays) after a date up to and including another. */
    private static int businessDaysBetween(java.util.Set<String> holidays, LocalDate from, LocalDate to) {
        int days = 0;
        for (LocalDate d = from.plusDays(1); !d.isAfter(to); d = d.plusDays(1)) {
            java.time.DayOfWeek day = d.getDayOfWeek();
            if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY && !holidays.contains(d.toString())) {
                days++;
            }
        }
        return days;
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects) {
        return add(sheetId, personId, amount, date, note, corrects, null);
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects, String ref) {
        return add(sheetId, personId, amount, date, note, corrects, ref, null);
    }

    /** Add a draft claim with an external reference (null: none) and tags (see {@link Claim#tags(String)}; null: none). */
    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, String corrects, String ref, String tags) {
        LocalDate dueBy = dueBy(sheetId, personId, date);
        boolean late = lateEntry(sheetId, dueBy);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects, due_by, late_entry, ref, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setString(6, corrects);
            ps.setDate(7, Date.valueOf(dueBy));
            ps.setBoolean(8, late);
            ps.setString(9, ref);
            ps.setString(10, tags);
            return ps;
        }, keys);
        kept(sheetId, keys.getKey().longValue(), true);
        used(sheetId, tags);
        return new Claim(keys.getKey().longValue(), personId, money(amount), date.toString(), note, "DRAFT", null, null, null, false, null, corrects,
                dueBy.toString(), false, null, false, 0, null, late, null, ref, tags, null, false, 0, false);
    }

    /** Whether a claim added now with a due-by is a late entry: the due-by falls in a soft-closed period of the sheet. */
    private boolean lateEntry(String sheetId, LocalDate date) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM period WHERE sheet_id = ? AND soft_closed"
                + " AND from_date <= ? AND to_date >= ?", Integer.class, sheetId, Date.valueOf(date), Date.valueOf(date));
        return n != null && n > 0;
    }

    /** Split an amount off an OPEN claim: the claim keeps the rest of its amount and is unbound from its last
     *  statement (live again); a new OPEN claim of the same person with the split-off amount, the same date and
     *  note and tags, opened by {@code by}, with its due-by fixed now and the original's number as split-from, is added
     *  (unnumbered: the caller numbers it; a late entry when its due-by falls in a soft-closed period). The new claim's id, or null when the sheet has no such OPEN claim. */
    public Long split(String sheetId, Claim claim, BigDecimal amount, String by) {
        BigDecimal rest = new BigDecimal(claim.amount()).subtract(amount);
        if (jdbc.update("UPDATE claim SET amount = ?, stated = FALSE, stated_due = NULL, stated_na = FALSE"
                + " WHERE id = ? AND sheet_id = ? AND status = 'OPEN'", rest, claim.id(), sheetId) == 0) {
            return null;
        }
        kept(sheetId, claim.id(), true);
        LocalDate date = LocalDate.parse(claim.date());
        LocalDate dueBy = dueBy(sheetId, claim.personId(), date);
        boolean late = lateEntry(sheetId, dueBy);
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, due_by, status, opened_by, split_from, late_entry, tags)"
                            + " VALUES (?, ?, ?, ?, ?, ?, 'OPEN', ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, claim.personId());
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, claim.note());
            ps.setDate(6, Date.valueOf(dueBy));
            ps.setString(7, by);
            ps.setString(8, claim.number());
            ps.setBoolean(9, late);
            ps.setString(10, claim.tags());
            return ps;
        }, keys);
        kept(sheetId, keys.getKey().longValue(), true);
        return keys.getKey().longValue();
    }

    /** Change a DRAFT claim's amount, date (with the due-by it now has), note, the number it corrects, its external reference (null: none) and
     *  its tags (null: none); false when the sheet has no such draft claim. */
    public boolean update(String sheetId, long claimId, BigDecimal amount, LocalDate date, LocalDate dueBy, String note, String corrects, String ref,
            String tags) {
        boolean changed = kept(sheetId, claimId, jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, due_by = ?, note = ?, corrects = ?, ref = ?,"
                + " tags = ? WHERE id = ? AND sheet_id = ? AND status = 'DRAFT'",
                amount, Date.valueOf(date), Date.valueOf(dueBy), note, corrects, ref, tags, claimId, sheetId) > 0);
        if (changed) used(sheetId, tags);
        return changed;
    }

    /** Note tags (comma separated; null: none) as used on the sheet: each stays in {@link #tagsUsed} from now on. */
    private void used(String sheetId, String tags) {
        if (tags == null) return;
        for (String tag : tags.split(",")) {
            jdbc.update("MERGE INTO sheet_tag (sheet_id, tag) KEY (sheet_id, tag) VALUES (?, ?)", sheetId, tag);
        }
    }

    /** Every tag ever used on the sheet, sorted. */
    public List<String> tagsUsed(String sheetId) {
        return jdbc.queryForList("SELECT tag FROM sheet_tag WHERE sheet_id = ? ORDER BY tag", String.class, sheetId);
    }

    /** Whether an external reference is already used by a claim on the sheet other than {@code exceptId} (null: any),
     *  comparing without regard to case. */
    public boolean refUsed(String sheetId, String ref, Long exceptId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND ref IS NOT NULL AND LOWER(ref) = LOWER(?)"
                + " AND id <> ?", Integer.class, sheetId, ref, exceptId == null ? -1L : exceptId);
        return n != null && n > 0;
    }

    /** Lock a claim for {@code by}'s editor, releasing any other claim of the same person {@code by} held;
     *  false when another user holds the claim's lock. */
    public boolean lock(String sheetId, Claim claim, String by) {
        if (jdbc.update("UPDATE claim SET locked_by = ? WHERE id = ? AND sheet_id = ?"
                + " AND (locked_by IS NULL OR locked_by = ?)", by, claim.id(), sheetId, by) == 0) {
            return false;
        }
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND person_id = ? AND locked_by = ? AND id <> ?",
                sheetId, claim.personId(), by, claim.id());
        return true;
    }

    /** Release a claim's lock when {@code by} holds it. */
    public void unlock(String sheetId, long claimId, String by) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE id = ? AND sheet_id = ? AND locked_by = ?", claimId, sheetId, by);
    }

    /** Release every claim lock on the sheet (its today changed). */
    public void unlockAll(String sheetId) {
        jdbc.update("UPDATE claim SET locked_by = NULL WHERE sheet_id = ? AND locked_by IS NOT NULL", sheetId);
    }

    /** Who holds a claim's lock; null when it is not locked. */
    public String lockedBy(String sheetId, long claimId) {
        List<String> found = jdbc.queryForList("SELECT locked_by FROM claim WHERE id = ? AND sheet_id = ?", String.class,
                claimId, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Make the sheet's next settle batch and give it to these claims: its number (1, 2, ... on the sheet). */
    public int batch(String sheetId, List<Long> claimIds) {
        jdbc.update("UPDATE sheet SET last_batch = COALESCE(last_batch, 0) + 1 WHERE id = ?", sheetId);
        int batch = lastBatch(sheetId);
        for (Long cid : claimIds) {
            jdbc.update("UPDATE claim SET batch = ? WHERE id = ? AND sheet_id = ?", batch, cid, sheetId);
        }
        return batch;
    }

    /** The number of the sheet's last settle batch; null when there has been none. */
    public Integer lastBatch(String sheetId) {
        List<Integer> found = jdbc.queryForList("SELECT last_batch FROM sheet WHERE id = ?", Integer.class, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The settle batch of each of the sheet's claims settled by one, by claim id. */
    public Map<String, Integer> batches(String sheetId) {
        Map<String, Integer> batches = new LinkedHashMap<>();
        jdbc.query("SELECT id, batch FROM claim WHERE sheet_id = ? AND batch IS NOT NULL",
                rs -> {
                    batches.put(String.valueOf(rs.getLong("id")), rs.getInt("batch"));
                }, sheetId);
        return batches;
    }

    /** Who holds each locked claim of the sheet, by claim id. */
    public Map<String, String> locks(String sheetId) {
        Map<String, String> locks = new LinkedHashMap<>();
        jdbc.query("SELECT id, locked_by FROM claim WHERE sheet_id = ? AND locked_by IS NOT NULL",
                rs -> {
                    locks.put(String.valueOf(rs.getLong("id")), rs.getString("locked_by"));
                }, sheetId);
        return locks;
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

    /** Whether a claim's status log last records it DELETED on the sheet's today. */
    public boolean deletedToday(String sheetId, long claimId) {
        List<Long> found = jdbc.query("SELECT DATEDIFF('DAY', l.on_date, s.today) AS days FROM claim_log l"
                + " JOIN sheet s ON s.id = l.sheet_id WHERE l.sheet_id = ? AND l.claim_id = ? AND l.status = 'DELETED'"
                + " ORDER BY l.id DESC LIMIT 1", (rs, i) -> rs.getLong("days"), sheetId, claimId);
        return !found.isEmpty() && found.get(0) == 0;
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

    /** Keep a comment on a claim: its text, who wrote it and the sheet's today. */
    public void comment(String sheetId, long claimId, String by, String text) {
        jdbc.update("INSERT INTO claim_comment (sheet_id, claim_id, by_user, on_date, text)"
                + " SELECT ?, ?, ?, today, ? FROM sheet WHERE id = ?", sheetId, claimId, by, text, sheetId);
    }

    /** The comments of the sheet's claims, by claim id: each oldest first, as "<user> on <date>: <text>". */
    public Map<String, List<String>> comments(String sheetId) {
        Map<String, List<String>> comments = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, by_user, on_date, text FROM claim_comment WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    comments.computeIfAbsent(String.valueOf(rs.getLong("claim_id")), k -> new ArrayList<>())
                            .add(rs.getString("by_user") + " on " + rs.getDate("on_date").toLocalDate() + ": " + rs.getString("text"));
                }, sheetId);
        return comments;
    }

    /** Add a line to a DRAFT claim and make its amount its lines' quantities times their unit amounts, added up: that
     *  amount, or null when the sheet has no such DRAFT claim (nothing is written then). */
    public BigDecimal addLine(String sheetId, long claimId, String label, int qty, BigDecimal unit) {
        Integer draft = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE id = ? AND sheet_id = ? AND status = 'DRAFT'",
                Integer.class, claimId, sheetId);
        if (draft == null || draft == 0) return null;
        jdbc.update("INSERT INTO claim_line (sheet_id, claim_id, label, qty, unit) VALUES (?, ?, ?, ?, ?)",
                sheetId, claimId, label, qty, unit);
        BigDecimal amount = linesTotal(sheetId, claimId);
        kept(sheetId, claimId, jdbc.update("UPDATE claim SET amount = ? WHERE id = ? AND sheet_id = ?", amount, claimId, sheetId) > 0);
        return amount;
    }

    /** A claim's lines' quantities times their unit amounts, added up; null when it has no lines. */
    public BigDecimal linesTotal(String sheetId, long claimId) {
        List<BigDecimal> found = jdbc.queryForList("SELECT SUM(qty * unit) FROM claim_line WHERE sheet_id = ? AND claim_id = ?"
                + " HAVING COUNT(*) > 0", BigDecimal.class, sheetId, claimId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).setScale(2);
    }

    /** The lines of the sheet's claims, by claim id: each oldest first, as "<label> <qty> x <unit>". */
    public Map<String, List<String>> lines(String sheetId) {
        Map<String, List<String>> lines = new LinkedHashMap<>();
        jdbc.query("SELECT claim_id, label, qty, unit FROM claim_line WHERE sheet_id = ? ORDER BY id",
                rs -> {
                    lines.computeIfAbsent(String.valueOf(rs.getLong("claim_id")), k -> new ArrayList<>())
                            .add(rs.getString("label") + " " + rs.getInt("qty") + " x " + money(rs.getBigDecimal("unit")));
                }, sheetId);
        return lines;
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

    /** Whether the user is the person's approver. */
    public boolean approver(String sheetId, long personId, String user) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ? AND approver = ?",
                Integer.class, sheetId, personId, user);
        return n != null && n > 0;
    }

    /** Whether a person's flag is in force: the sheet's today on or before its until-date. */
    public boolean flagged(String sheetId, long personId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ? AND flag_until"
                + " >= (SELECT s.today FROM sheet s WHERE s.id = person.sheet_id)", Integer.class, sheetId, personId);
        return n != null && n > 0;
    }
}
