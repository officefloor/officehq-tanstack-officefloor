package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private static final String COLUMNS = "id, person_id, amount, claim_date, note, status, claim_no, settled_due, opened_by, stated, stated_due, corrects";

    private static final RowMapper<Claim> ROW = (rs, i) -> new Claim(rs.getLong("id"), rs.getLong("person_id"),
            money(rs.getBigDecimal("amount")), rs.getDate("claim_date").toLocalDate().toString(),
            rs.getString("note"), rs.getString("status"),
            rs.getObject("claim_no", Long.class), dueOf(rs.getBigDecimal("settled_due")),
            rs.getString("opened_by"), rs.getBoolean("stated"), dueOf(rs.getBigDecimal("stated_due")),
            rs.getObject("corrects", Long.class));

    /** The grade a person (sheet, person id) has on a date: the one with the latest from-date on
     *  or before it, the first one they were given applying from the beginning of time. */
    private static final String GRADE_ON = "(SELECT g.grade FROM person_grade g WHERE g.sheet_id = ? AND g.person_id = ?"
            + " AND (g.from_date IS NULL OR g.from_date <= CAST(? AS DATE)) ORDER BY g.from_date DESC NULLS LAST LIMIT 1)";

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean personExists(String sheetId, long personId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE sheet_id = ? AND id = ?",
                Integer.class, sheetId, personId);
        return known != null && known > 0;
    }

    /** The sheet's claims, newest first. */
    public List<Claim> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? ORDER BY id DESC", ROW, sheetId);
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
    public long number(String sheetId, long claimId) {
        jdbc.update("UPDATE sheet SET claim_number = claim_number + 1 WHERE id = ?", sheetId);
        Long next = jdbc.queryForObject("SELECT claim_number FROM sheet WHERE id = ?", Long.class, sheetId);
        jdbc.update("UPDATE claim SET claim_no = ? WHERE id = ? AND sheet_id = ?", next, claimId, sheetId);
        return next;
    }

    /** Record who opened a claim. */
    public void openedBy(String sheetId, long claimId, String by) {
        jdbc.update("UPDATE claim SET opened_by = ? WHERE id = ? AND sheet_id = ?", by, claimId, sheetId);
    }

    /** A claim's due now: its amount at the rate, in force on the claim's date, for the grade its person
     *  had on that date
     *  (the one with the latest from-date on or before it; an undated rate applies from the beginning
     *  of time), rounded to two decimals by the claim's rounding (see {@link #rounding}), and at least the sheet's floor (a correction's, a
     *  negative amount's, has no floor); null when no rate applies. */
    public BigDecimal due(String sheetId, Claim claim) {
        List<BigDecimal> pct = jdbc.queryForList("SELECT r.pct FROM rate r WHERE r.sheet_id = ? AND r.grade = "
                + GRADE_ON + " AND (r.from_date IS NULL OR r.from_date <= CAST(? AS DATE))"
                + " ORDER BY r.from_date DESC NULLS LAST LIMIT 1", BigDecimal.class, sheetId,
                sheetId, claim.personId(), claim.date(), claim.date());
        if (pct.isEmpty()) return null;
        BigDecimal due = new BigDecimal(claim.amount()).multiply(pct.get(0)).movePointLeft(2)
                .setScale(2, rounding(sheetId, claim));
        return new BigDecimal(claim.amount()).signum() < 0 ? due : due.max(floor(sheetId));
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

    /** The sheet's floor: the least a live due can be (two decimals). */
    public BigDecimal floor(String sheetId) {
        List<BigDecimal> found = jdbc.queryForList("SELECT floor FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ONE.setScale(2) : found.get(0).setScale(2);
    }

    /** A claim's figure now: the due fixed when it was settled, else the one its last statement bound
     *  it to, else its live due; null when it has none. */
    public BigDecimal figure(String sheetId, Claim claim) {
        if (claim.due() != null) return new BigDecimal(claim.due());
        if (claim.stated() && claim.statedDue() != null) return new BigDecimal(claim.statedDue());
        return due(sheetId, claim);
    }

    /** A claim's due now: its figure, plus, while it is overdue (OPEN and its due-by — its date plus the
     *  sheet's terms — before the sheet's today), the sheet's late percent of that figure for every full
     *  30 days it is overdue, rounded to two decimals by the claim's rounding. A statement never binds the charge:
     *  it keeps following the sheet's today until settling freezes it in. A correction (a negative amount)
     *  has no late charge. Null when it has no figure. */
    public BigDecimal dueNow(String sheetId, Claim claim) {
        BigDecimal figure = figure(sheetId, claim);
        if (figure == null || !claim.status().equals("OPEN") || new BigDecimal(claim.amount()).signum() < 0) {
            return figure;
        }
        List<Object[]> found = jdbc.query("SELECT today, terms, late_pct FROM sheet WHERE id = ?",
                (rs, i) -> new Object[] { rs.getDate("today").toLocalDate(), rs.getInt("terms"),
                        rs.getBigDecimal("late_pct") }, sheetId);
        if (found.isEmpty()) return figure;
        LocalDate today = (LocalDate) found.get(0)[0];
        LocalDate dueBy = LocalDate.parse(claim.date()).plusDays((Integer) found.get(0)[1]);
        BigDecimal pct = (BigDecimal) found.get(0)[2];
        long periods = dueBy.isBefore(today) ? ChronoUnit.DAYS.between(dueBy, today) / 30 : 0;
        if (periods == 0 || pct == null || pct.signum() == 0) return figure;
        BigDecimal charge = figure.multiply(pct).multiply(BigDecimal.valueOf(periods)).movePointLeft(2)
                .setScale(2, rounding(sheetId, claim));
        return figure.add(charge);
    }

    /** Whether a rate for the grade the person has on a date is in force on that date. */
    public boolean rated(String sheetId, long personId, LocalDate on) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rate r WHERE r.sheet_id = ? AND r.grade = " + GRADE_ON
                + " AND (r.from_date IS NULL OR r.from_date <= CAST(? AS DATE))", Integer.class, sheetId,
                sheetId, personId, on.toString(), on.toString());
        return n != null && n > 0;
    }

    /** Bind a claim to a figure (stated), or unbind it (not stated, live again). */
    public void bind(String sheetId, long claimId, boolean stated, BigDecimal figure) {
        jdbc.update("UPDATE claim SET stated = ?, stated_due = ? WHERE id = ? AND sheet_id = ?", stated, figure,
                claimId, sheetId);
    }

    /** A person's claims on the sheet. */
    public List<Claim> ofPerson(String sheetId, long personId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM claim WHERE sheet_id = ? AND person_id = ? ORDER BY id DESC",
                ROW, sheetId, personId);
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
    public boolean settledOf(String sheetId, long personId, long number) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM claim WHERE sheet_id = ? AND person_id = ?"
                + " AND claim_no = ? AND status = 'SETTLED'", Integer.class, sheetId, personId, number);
        return n != null && n > 0;
    }

    public Claim add(String sheetId, long personId, BigDecimal amount, LocalDate date, String note, Long corrects) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claim (sheet_id, person_id, amount, claim_date, note, corrects) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, sheetId);
            ps.setLong(2, personId);
            ps.setBigDecimal(3, amount);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, note);
            ps.setObject(6, corrects);
            return ps;
        }, keys);
        return new Claim(keys.getKey().longValue(), personId, money(amount), date.toString(), note, "DRAFT", null, null, null, false, null, corrects);
    }

    /** Change a DRAFT claim's amount, date, note and the number it corrects; false when the sheet has no
     *  such draft claim. */
    public boolean update(String sheetId, long claimId, BigDecimal amount, LocalDate date, String note, Long corrects) {
        return jdbc.update("UPDATE claim SET amount = ?, claim_date = ?, note = ?, corrects = ? WHERE id = ?"
                + " AND sheet_id = ? AND status = 'DRAFT'",
                amount, Date.valueOf(date), note, corrects, claimId, sheetId) > 0;
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
