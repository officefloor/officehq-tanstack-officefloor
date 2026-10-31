package net.officefloor.hq.app.person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

/** A person on a sheet. `id` is null until the person is first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    /** The person's grades by the date each is in force from; LocalDate.MIN for the first one,
     *  which applies from the beginning of time. */
    private TreeMap<LocalDate, String> grades = new TreeMap<>();
    /** The sheet's today as it was when the person was read (null: the person is new). */
    private LocalDate today;
    /** A grade this save records, from its date (null: the save records none). */
    private DatedGrade newGrade;
    private int version;
    private int statementCount;
    private BigDecimal statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    private List<Hold> holds = new ArrayList<>();
    private List<Long> patronIds = new ArrayList<>();

    public Person(Long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(Long id, String sheetId, String name, String grade, int version) {
        this.version = version;
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        if (grade != null) this.grades.put(LocalDate.MIN, grade);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    /** The grade in force on the sheet's today. */
    public String getGrade() { return today == null ? grades.lastEntry().getValue() : gradeOn(today); }
    /** The grade in force on `date`: the one from the latest date on or before it, the first one
     *  applying from the beginning of time. */
    public String gradeOn(LocalDate date) {
        Map.Entry<LocalDate, String> e = grades.floorEntry(date);
        return e != null ? e.getValue() : grades.firstEntry().getValue();
    }
    /** The first grade the person was given. */
    public String getFirstGrade() { return grades.firstEntry().getValue(); }
    public TreeMap<LocalDate, String> getGrades() { return grades; }
    public void setGrades(TreeMap<LocalDate, String> grades) { this.grades = grades; }
    public void setToday(LocalDate today) { this.today = today; }
    public DatedGrade getNewGrade() { return newGrade; }
    /** Record `grade` from `from` with the next save, replacing any grade from that same date. */
    public void recordGrade(String grade, LocalDate from) {
        this.newGrade = new DatedGrade(grade, from);
        LocalDate key = grades.isEmpty() ? LocalDate.MIN : from;
        grades.put(key, grade);
    }
    /** How many times the person has been saved since added. */
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    /** How many statements have been taken for the person. */
    public int getStatementCount() { return statementCount; }
    public void setStatementCount(int statementCount) { this.statementCount = statementCount; }
    /** The last statement's due-total; null when none was taken or it was n/a. */
    public BigDecimal getStatedTotal() { return statedTotal; }
    public void setStatedTotal(BigDecimal statedTotal) { this.statedTotal = statedTotal; }
    /** The sheet's today when the last statement was taken; null when none was. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    /** The date the person is retired from; null when they are not retired. Set at all, they count
     *  as retired at once, whatever the date. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** Whether the person has any hold, whatever its dates. */
    public boolean isHeld() { return !holds.isEmpty(); }
    /** The person's holds, in the order they were added. */
    public List<Hold> getHolds() { return holds; }
    public void setHolds(List<Hold> holds) { this.holds = holds; }
    /** Whether `date` falls within any of the person's holds. */
    public boolean heldOn(LocalDate date) {
        return holds.stream().anyMatch(h -> h.covers(date));
    }
    /** The ids of the people the person has named as patrons, in the order they were named. */
    public List<Long> getPatronIds() { return patronIds; }
    public void setPatronIds(List<Long> patronIds) { this.patronIds = patronIds; }

    /** Whether a claim of the person's counts in the live totals: OPEN, PENDING and SETTLED ones do,
     *  but not a deferred one, nor an OPEN or PENDING one dated within one of the person's holds, unless a statement binds it. */
    public boolean counts(Claim claim) {
        return counts(claim, claim.getStatus());
    }

    /** Whether a claim of the person's counts as it would with `status` (null: none, it does not
     *  count): by the live rules above, but with that status in place of the claim's own. */
    public boolean counts(Claim claim, ClaimStatus status) {
        if (status == null) return false;
        if (!status.counts() || claim.isDeferred()) return false;
        if (!status.isOpen() || !heldOn(claim.getDate())) return true;
        return claim.isStated() && status != ClaimStatus.PENDING;
    }

    /** Refuses a claim dated after the person's retirement date. */
    public void refuseRetired(LocalDate claimDate) throws PersonRetiredException {
        if (retiredFrom != null && claimDate.isAfter(retiredFrom)) {
            throw new PersonRetiredException(name, retiredFrom);
        }
    }

    /** A grade in force from a date (null: from the beginning of time). */
    public record DatedGrade(String grade, LocalDate from) {
    }
}
