package net.officefloor.hq.app.sheet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A sheet, chosen by its id; everything else in the app is kept per sheet. */
@Entity
@Table(name = "sheet")
public class Sheet {

    /** The keys of the dated settings: those that carry a figure. */
    public static final String FLOOR = "floor", LATE_PCT = "late-pct", TERMS = "terms", CLERK_LIMIT = "clerk-limit",
            CAP = "cap", PROOF_THRESHOLD = "proof-threshold";

    /** The key of the grace days: undated, a whole number of days (null: off). */
    public static final String GRACE_DAYS = "grace-days";

    /** The setting key of an action's entry in the permission matrix. */
    public static String permKey(String action) {
        return "perm-" + action;
    }

    @Id
    private String id;

    /** The sheet's today: every date decision and default date uses it, never the real clock. */
    @Column(nullable = false)
    private LocalDate today;

    /** The dated settings: each setting that carries a figure — the clerk limit (a clerk settles a claim only when
     *  its due is at or below it), the floor (a claim's live due is at least it), the terms and each grade's own terms
     *  (key terms-&lt;grade&gt;; a claim is due by its date plus that many days), the late percent (an overdue claim's due
     *  gains, for each full 30 days overdue, the one in force on that boundary date of its figure), the cap (a person's due-total is at most it) and the
     *  proof threshold (a claim whose due is above the one in force on its date cannot be settled or approved
     *  without a proof) —
     *  recorded from a from-date on. */
    @OneToMany(mappedBy = "sheet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SheetSetting> settings = new ArrayList<>();

    /** The policy date: a claim dated before it rounds half up, one on or after it half to even; null (blank) for
     *  the sheet's today. */
    @Column(name = "policy_date")
    private LocalDate policyDate;

    /** The period numbering from: while the sheet's today is on or after it, a claim opened is numbered within its
     *  period; null (blank) for off. */
    @Column(name = "period_numbering_from")
    private LocalDate periodNumberingFrom;

    /** The late percent a sheet starts at. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The terms a sheet starts at. */
    public static final int DEFAULT_TERMS = 14;

    /** The floor a sheet starts at. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    protected Sheet() {
    }

    public Sheet(String id) {
        this.id = id;
        this.today = LocalDate.now(); // a new sheet starts on the real date
    }

    public String getId() {
        return id;
    }

    public LocalDate getToday() {
        return today;
    }

    public void setToday(LocalDate today) {
        this.today = today;
    }

    /** The dated settings recorded, by key, then by from-date (one from the beginning of time first). */
    public List<SheetSetting> getSettings() {
        return settings.stream()
                .sorted(Comparator.comparing(SheetSetting::getKey)
                        .thenComparing(SheetSetting::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    /** Records a setting's value from a date on (null: blank), replacing the value already recorded from that date. */
    public void record(String key, LocalDate from, String value) {
        for (SheetSetting s : settings) {
            if (s.getKey().equals(key) && Objects.equals(s.getFromDate(), from)) {
                s.setValue(value);
                return;
            }
        }
        settings.add(new SheetSetting(this, key, from, value));
    }

    /** The setting recorded for a key in force on a date: the one with the latest from-date on or before it; none
     *  before every from-date. */
    private Optional<SheetSetting> settingOn(String key, LocalDate on) {
        return settings.stream()
                .filter(s -> s.getKey().equals(key))
                .filter(s -> s.getFromDate() == null || !s.getFromDate().isAfter(on))
                .max(Comparator.comparing(SheetSetting::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    /** The value of a setting in force on a date, as recorded (null: blank, or none recorded). */
    public String valueOn(String key, LocalDate on) {
        return settingOn(key, on).map(SheetSetting::getValue).orElse(null);
    }

    private BigDecimal amountOn(String key, LocalDate on) {
        String value = valueOn(key, on);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The clerk limit in force on the sheet's today; null when there is none. */
    public BigDecimal getClerkLimit() {
        return amountOn(CLERK_LIMIT, today);
    }

    /** The floor in force on a date; the starting 1.00 before every from-date. */
    public BigDecimal floorOn(LocalDate on) {
        BigDecimal floor = amountOn(FLOOR, on);
        return floor != null ? floor : DEFAULT_FLOOR;
    }

    /** The terms in force on a date; the starting 14 before every from-date. */
    public int termsOn(LocalDate on) {
        String terms = valueOn(TERMS, on);
        return terms != null ? Integer.parseInt(terms) : DEFAULT_TERMS;
    }

    /** The late percent in force on a date; the starting 0.00 before every from-date. */
    public BigDecimal latePctOn(LocalDate on) {
        BigDecimal latePct = amountOn(LATE_PCT, on);
        return latePct != null ? latePct : DEFAULT_LATE_PCT;
    }

    /** The grade's own terms in days in force on a date (grades A, B and C); null when it has none and uses the
     *  sheet's terms. */
    public Integer gradeTermsOn(String grade, LocalDate on) {
        String terms = valueOn(TERMS + "-" + grade, on);
        return terms != null ? Integer.valueOf(terms) : null;
    }

    /** The terms in force for a grade on a date: its own, else the sheet's. */
    public int termsFor(String grade, LocalDate on) {
        Integer own = grade == null ? null : gradeTermsOn(grade, on);
        return own != null ? own : termsOn(on);
    }

    /** The cap in force on the sheet's today; null when there is none. */
    public BigDecimal getCap() {
        return amountOn(CAP, today);
    }

    /** The proof threshold in force on a date; null when there is none. */
    public BigDecimal proofThresholdOn(LocalDate on) {
        return amountOn(PROOF_THRESHOLD, on);
    }

    /** The permission matrix entry for an action as recorded: its comma separated roles; "" when blank (the
     *  standing rule applies). Not dated. */
    public String permOf(String action) {
        String roles = valueOn(permKey(action), today);
        return roles == null ? "" : roles;
    }

    /** Whether a role may take an action: by the roles of the action's permission matrix entry, else by the
     *  standing rule — open, settle, void and close by clerk and steward; reassign and reopen by steward. */
    public boolean allows(String action, String role) {
        String roles = permOf(action);
        if (roles.isEmpty()) {
            return "steward".equals(role) || !("reassign".equals(action) || "reopen".equals(action));
        }
        return List.of(roles.split(",")).contains(role);
    }

    /** The grace days: once the sheet's today is changed, an open period whose to-date plus them is before it is
     *  closed. Not dated; null when off. */
    public Integer getGraceDays() {
        String days = valueOn(GRACE_DAYS, today);
        return days == null ? null : Integer.valueOf(days);
    }

    public LocalDate getPolicyDate() {
        return policyDate;
    }

    public void setPolicyDate(LocalDate policyDate) {
        this.policyDate = policyDate;
    }

    /** The policy date in force: the one set, else the sheet's today. */
    public LocalDate policyDateInForce() {
        return policyDate != null ? policyDate : today;
    }

    public LocalDate getPeriodNumberingFrom() {
        return periodNumberingFrom;
    }

    public void setPeriodNumberingFrom(LocalDate periodNumberingFrom) {
        this.periodNumberingFrom = periodNumberingFrom;
    }

    /** Whether a claim opened now is numbered within its period: the period numbering is set and today is on or after it. */
    public boolean periodNumberingOn() {
        return periodNumberingFrom != null && !today.isBefore(periodNumberingFrom);
    }
}
