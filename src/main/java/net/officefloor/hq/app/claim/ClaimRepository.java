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

    private static final String COLUMNS = "id, person_id, amount, claim_date, note, status, claim_no, settled_due, opened_by, stated, stated_due, corrects, due_by, stated_na, deferred_until, proofs, split_from, late_entry, reminded, ref, tags, void_reason,"
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
            rs.getString("ref"), rs.getString("tags"), rs.getString("void_reason"));

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

    /** The given claims as the sheet lists them: people in the order they were added, each person's claims
     *  newest first (a reassigned claim counts as its new person's newest); ids the sheet does not have are left out. */
    public List<Claim> inListOrder(String sheetId, Collection<Long> ids) {
        List<Claim> out = new ArrayList<>();
        for (Claim c : list(sheetId)) {
            if (ids.contains(c.id())) out.add(c);
        }
        out.sort(Comparator.comparingLong(Claim::personId));
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

    /** Record the reason a claim was voided with. */
    public void voidReason(String sheetId, long claimId, String reason) {
        kept(sheetId, claimId, jdbc.update("UPDATE claim SET void_reason = ? WHERE id = ? AND sheet_id = ?", reason, claimId, sheetId) > 0);
    }

    /** Give an opened claim the sheet's next number. The sheet's row stays locked until the
     *  transaction commits, so two claims opened at once get consecutive numbers. */
    public String number(String sheetId, long claimId) {
        jdbc.update("UPDATE sheet SET claim_number = claim_number + 1 WHERE id = ?", sheetId);
        Long next = jdbc.queryForObject("SELECT claim_number FROM sheet WHERE id = ?", Long.class, sheetId);
        String number = String.valueOf(next);
        jdbc.update("UPDATE claim SET claim_no = ? WHERE id = ? AND sheet_id = ?", number, claimId, sheetId);
        kept(sheetId, claimId, true);
        return number;
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
     *  is overdue (OPEN or PENDING and its due-by — fixed when it was created — plus its grade's grace days before the
     *  sheet's today), for every full 30 days it is overdue, the sheet's late percent in force on that boundary date (the
     *  due-by plus the grace plus 30, 60,
     *  90 ... days) of that figure plus the charges of the boundaries before it (compounding), each rounded to two decimals by the claim's rounding, the charges added up (a
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

    /** Whether a claim is overdue on a day: open, not deferred, and its overdue-from day (see {@link #overdueFrom})
     *  before that day. */
    public boolean overdue(String sheetId, Claim claim, LocalDate today) {
        return claim.open() && !claim.deferred() && overdueFrom(sheetId, claim).isBefore(today);
    }

    /** The late charge on a claim's figure now (see {@link #dueNow}); zero when it is not overdue. */
    private BigDecimal lateCharge(String sheetId, Claim claim, BigDecimal figure) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (!claim.open() || claim.deferred()) return charge;
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

    /** Move every claim of one person to another (a merge): each keeps its number and date, loses any figure a
     *  statement bound it to, and is listed after the other's claims in the order it was added. */
    public void moveAll(String sheetId, long fromId, long intoId) {
        List<Long> ids = jdbc.queryForList("SELECT id FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id",
                Long.class, sheetId, fromId);
        for (Long claimId : ids) {
            kept(sheetId, claimId, jdbc.update("UPDATE claim SET person_id = ?, stated = FALSE, stated_due = NULL,"
                    + " stated_na = FALSE, listed = NEXT VALUE FOR claim_listed_seq WHERE id = ? AND sheet_id = ?",
                    intoId, claimId, sheetId) > 0);
        }
    }

    /** Defer an OPEN claim until a date; false when the sheet has no such OPEN claim. */
    public boolean defer(String sheetId, long claimId, LocalDate until) {
        return kept(sheetId, claimId, jdbc.update("UPDATE claim SET deferred_until = ? WHERE id = ? AND sheet_id = ? AND status = 'OPEN'",
                Date.valueOf(until), claimId, sheetId) > 0);
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

    /** Remind an OPEN or PENDING claim on the sheet's today, only when it was not already reminded that day;
     *  false when the sheet has no such claim. */
    public boolean remind(String sheetId, long claimId) {
        return jdbc.update("UPDATE claim c SET reminded = (SELECT s.today FROM sheet s WHERE s.id = c.sheet_id)"
                + " WHERE c.id = ? AND c.sheet_id = ? AND c.status IN ('OPEN', 'PENDING')"
                + " AND (c.reminded IS NULL OR c.reminded <> (SELECT s.today FROM sheet s WHERE s.id = c.sheet_id))",
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

    /** The due-by of a claim created now for a person with a date: the terms-th business day after the date (skipping
     *  Saturdays, Sundays and the sheet's holidays), for the person's own terms, else the terms, in force on it, of the
     *  grade the person has on it, else the sheet's terms in force on it. */
    public LocalDate dueBy(String sheetId, long personId, LocalDate date) {
        List<Integer> mine = jdbc.queryForList("SELECT terms FROM person WHERE sheet_id = ? AND id = ?", Integer.class,
                sheetId, personId);
        if (!mine.isEmpty() && mine.get(0) != null) {
            return settings.businessDaysAfter(sheetId, date, mine.get(0));
        }
        List<String> grade = jdbc.queryForList("SELECT " + GRADE_ON + " FROM DUAL", String.class, sheetId, personId,
                date.toString());
        return dueByOfGrade(sheetId, grade.isEmpty() ? null : grade.get(0), date);
    }

    /** The due-by of a claim created now with a date for a person of a grade (null when none) with no terms of their
     *  own: the terms-th business day after the date (skipping Saturdays, Sundays and the sheet's holidays), for the
     *  grade's terms in force on it, else the sheet's terms in force on it. */
    public LocalDate dueByOfGrade(String sheetId, String grade, LocalDate date) {
        Integer own = grade == null ? null : settings.gradeTerms(sheetId, grade, date);
        return settings.businessDaysAfter(sheetId, date, own != null ? own : settings.terms(sheetId, date));
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
                dueBy.toString(), false, null, false, 0, null, late, null, ref, tags, null);
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
