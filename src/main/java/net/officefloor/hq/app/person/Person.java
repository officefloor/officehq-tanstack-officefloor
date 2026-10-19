package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

/** A person on a sheet. Mutable so an Apply step changes it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private List<DatedGrade> grades = new ArrayList<>();
    private long version;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    private List<Hold> holds = new ArrayList<>();
    private List<String> patrons = new ArrayList<>();

    public Person(long id, String sheetId, String name, String grade, long version) {
        this(id, sheetId, name, grade, version, 0, null, null);
    }

    public Person(long id, String sheetId, String name, String grade, long version, int statementCount,
            BigDecimal statedTotal, LocalDate statementDate) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grades.add(new DatedGrade(grade, null));
        this.version = version;
        this.statementCount = statementCount;
        this.statedTotal = statedTotal;
        this.statementDate = statementDate;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    /** The person's grades, each in force from its from-date, earliest first (the first one from the
     *  beginning of time). */
    public List<DatedGrade> getGrades() { return grades; }
    public void setGrades(List<DatedGrade> grades) {
        this.grades = new ArrayList<>(grades);
        this.grades.sort(Comparator.comparing(DatedGrade::from, Comparator.nullsFirst(Comparator.naturalOrder())));
    }
    /** The grade in force on the date: the one with the latest from-date on or before it. */
    public String gradeOn(LocalDate date) {
        String grade = grades.get(0).grade();
        for (DatedGrade dated : grades) {
            if (dated.from() == null || !dated.from().isAfter(date)) grade = dated.grade();
        }
        return grade;
    }
    /** Records the grade from the date, keeping the ones already recorded (replacing one from the same date). */
    public void gradeFrom(String grade, LocalDate from) {
        List<DatedGrade> next = new ArrayList<>(grades);
        next.removeIf(dated -> from.equals(dated.from()));
        next.add(new DatedGrade(grade, from));
        setGrades(next);
    }
    /** The version as loaded from the store; moves on once the person is saved. */
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    /** The last statement's due-total: null when none was taken, or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    /** The sheet's today when the last statement was taken: null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    /** The date the person is retired from: null when they are not retired. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    /** The person's holds, in the order they were added. */
    public List<Hold> getHolds() { return holds; }
    public void setHolds(List<Hold> holds) { this.holds = new ArrayList<>(holds); }
    /** Whether the person has any hold. */
    public boolean isHeld() { return !holds.isEmpty(); }
    /** The names of the person's patrons, sorted. */
    public List<String> getPatrons() { return patrons; }
    public void setPatrons(List<String> patrons) { this.patrons = new ArrayList<>(patrons); }
    /** Whether this claim of the person counts in the live totals: SETTLED, and OPEN or PENDING unless
     *  it is dated within one of their holds and not bound by a statement. */
    public boolean counts(Claim claim) {
        ClaimStatus status = claim.getStatus();
        if (status == ClaimStatus.SETTLED) return true;
        if (!status.isOpen()) return false;
        return claim.isBound() || holds.stream().noneMatch(hold -> hold.covers(claim.getDate()));
    }
    /** Whether a claim of the person may be dated this day: not after their retirement date. */
    public boolean claimAllowedOn(LocalDate date) { return retiredFrom == null || !date.isAfter(retiredFrom); }
}
