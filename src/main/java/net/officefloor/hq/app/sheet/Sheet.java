package net.officefloor.hq.app.sheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A sheet, chosen by its id; everything else in the app is kept per sheet. */
@Entity
@Table(name = "sheet")
public class Sheet {

    @Id
    private String id;

    /** The sheet's today: every date decision and default date uses it, never the real clock. */
    @Column(nullable = false)
    private LocalDate today;

    /** The clerk limit: a clerk settles a claim only when its due is at or below it; null when there is none. */
    @Column(name = "clerk_limit", precision = 19, scale = 2)
    private BigDecimal clerkLimit;

    /** The floor: a claim's live due is at least it; a sheet starts at 1.00. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal floor = Sheet.DEFAULT_FLOOR;

    /** The terms: a claim is due by its date plus this many days; a sheet starts at 14. */
    @Column(nullable = false)
    private int terms = Sheet.DEFAULT_TERMS;

    /** Grade A's own terms in days; null (blank) when it uses the sheet's terms. */
    @Column(name = "terms_a")
    private Integer termsA;

    /** Grade B's own terms in days; null (blank) when it uses the sheet's terms. */
    @Column(name = "terms_b")
    private Integer termsB;

    /** Grade C's own terms in days; null (blank) when it uses the sheet's terms. */
    @Column(name = "terms_c")
    private Integer termsC;

    /** The late percent: an overdue claim's due gains it of its figure for every full 30 days overdue; a sheet starts at 0.00. */
    @Column(name = "late_pct", nullable = false, precision = 19, scale = 2)
    private BigDecimal latePct = Sheet.DEFAULT_LATE_PCT;

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

    public BigDecimal getClerkLimit() {
        return clerkLimit;
    }

    public void setClerkLimit(BigDecimal clerkLimit) {
        this.clerkLimit = clerkLimit;
    }

    public BigDecimal getFloor() {
        return floor;
    }

    public void setFloor(BigDecimal floor) {
        this.floor = floor;
    }

    public int getTerms() {
        return terms;
    }

    public void setTerms(int terms) {
        this.terms = terms;
    }

    public BigDecimal getLatePct() {
        return latePct;
    }

    public void setLatePct(BigDecimal latePct) {
        this.latePct = latePct;
    }

    /** The grade's own terms in days (grades A, B and C); null when it has none and uses the sheet's terms. */
    public Integer getGradeTerms(String grade) {
        return switch (grade) {
            case "A" -> termsA;
            case "B" -> termsB;
            case "C" -> termsC;
            default -> null;
        };
    }

    public void setGradeTerms(String grade, Integer terms) {
        switch (grade) {
            case "A" -> termsA = terms;
            case "B" -> termsB = terms;
            case "C" -> termsC = terms;
            default -> throw new IllegalArgumentException("No such grade: " + grade);
        }
    }

    /** The terms in force for a grade: its own, else the sheet's. */
    public int termsFor(String grade) {
        Integer own = grade == null ? null : getGradeTerms(grade);
        return own != null ? own : terms;
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
