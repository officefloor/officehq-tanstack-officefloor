package net.officefloor.hq.app.person;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

/** A person on a sheet: a name and their grades (A, B or C), each in force from its from-date (the
 *  first from the beginning of time), with {@code grade} the one in force on the sheet's today, the version raised by each saved edit, and
 *  their statements: how many have been taken, and the last one's due-total and date (none until the
 *  first), and the date they are retired from (null: not retired; set at all, they count as retired),
 *  and their holds, by when they were added (their OPEN claims dated within one, and not bound by a
 *  statement, count in no live total). */
public class Person {
    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    /** The grades they were given, by from-date (none first). */
    private java.util.List<GradeFrom> grades = new java.util.ArrayList<>();
    /** The grade this request's save records (null: none). */
    private GradeFrom recorded;
    /** Their own terms in whole days (null: none). */
    private Integer terms;
    /** Whether this request's save changed their terms, for its audit line. */
    private boolean termsChanged;
    private long version;
    private int statementCount;
    private String statedTotal;
    private String statementDate;
    private String retiredFrom;
    /** Their holds, in the order they were added. */
    private java.util.List<Hold> holds = new java.util.ArrayList<>();
    /** The ids of the people they name as patrons, in the order named. */
    private java.util.List<Long> patronIds = new java.util.ArrayList<>();
    /** How many claims the statement this request took bound, for its audit line. */
    private int statedClaims;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this(id, sheetId, name, grade, version, 0, null, null);
    }

    public Person(long id, String sheetId, String name, String grade, long version,
            int statementCount, String statedTotal, String statementDate) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
        this.statementCount = statementCount;
        this.statedTotal = statedTotal;
        this.statementDate = statementDate;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public java.util.List<GradeFrom> getGrades() { return grades; }

    /** Their grades, by from-date (none first), and the one in force on the sheet's today. */
    public void setGrades(java.util.List<GradeFrom> grades, java.time.LocalDate today) {
        this.grades = grades;
        this.grade = gradeOn(today.toString());
    }

    /** The grade in force on the date (yyyy-MM-dd): the one with the latest from-date on or before
     *  it; null when there is none. */
    public String gradeOn(String date) {
        GradeFrom found = null;
        for (GradeFrom g : grades) {
            if (g.from() != null && g.from().compareTo(date) > 0) continue;
            if (found == null || (found.from() == null ? "" : found.from()).compareTo(g.from() == null ? "" : g.from()) < 0) found = g;
        }
        return found == null ? null : found.grade();
    }

    /** Give them the grade from the from-date (null: the beginning of time), replacing the one
     *  with the same from-date; the save records it. */
    public void giveGrade(String grade, String from, java.time.LocalDate today) {
        java.util.List<GradeFrom> now = new java.util.ArrayList<>(grades);
        now.removeIf((g) -> java.util.Objects.equals(g.from(), from));
        now.add(new GradeFrom(grade, from));
        now.sort(java.util.Comparator.comparing((GradeFrom g) -> g.from() == null ? "" : g.from()));
        setGrades(now, today);
        recorded = new GradeFrom(grade, from);
    }

    public Integer getTerms() { return terms; }
    public void setTerms(Integer terms) { this.terms = terms; }

    /** Give them these terms (null: none); the audit line records them when they changed. */
    public void giveTerms(Integer terms) {
        termsChanged = !java.util.Objects.equals(this.terms, terms);
        this.terms = terms;
    }

    @JsonIgnore
    public boolean isTermsChanged() { return termsChanged; }

    @JsonIgnore
    public GradeFrom getRecorded() { return recorded; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public int getStatementCount() { return statementCount; }
    public String getStatedTotal() { return statedTotal; }
    public String getStatementDate() { return statementDate; }
    public String getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(String retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** Whether they have any hold. */
    public boolean isHeld() { return !holds.isEmpty(); }
    public java.util.List<Hold> getHolds() { return holds; }
    public void setHolds(java.util.List<Hold> holds) { this.holds = holds; }
    public java.util.List<Long> getPatronIds() { return patronIds; }
    public void setPatronIds(java.util.List<Long> patronIds) { this.patronIds = patronIds; }

    /** Whether the claim counts in live totals: it counts by status, and is not an open (OPEN or
     *  PENDING) claim dated within one of their holds that no statement bound, nor a deferred one. */
    public boolean countsLive(Claim claim) {
        return claim.isCounting() && !leftOutLive(claim);
    }

    /** Whether the live rules leave the claim out whatever its status counts: it is deferred, or an
     *  open (OPEN or PENDING) claim dated within one of their holds that no statement bound. */
    public boolean leftOutLive(Claim claim) {
        return claim.isDeferred()
                || (ClaimStatus.isOpen(claim.getStatus()) && !claim.isStated()
                        && holds.stream().anyMatch((h) -> h.covers(claim.getDate())));
    }

    /** Whether a claim on this date is refused: the person is retired from a date before it. */
    public boolean retiredBefore(java.time.LocalDate date) {
        return retiredFrom != null && date.isAfter(java.time.LocalDate.parse(retiredFrom));
    }

    /** Record a statement just taken: its due-total (null: n/a), date and how many claims it bound. */
    public void stated(String total, String date, int claims) {
        this.statementCount++;
        this.statedTotal = total == null ? "n/a" : total;
        this.statementDate = date;
        this.statedClaims = claims;
    }

    @JsonIgnore
    public int getStatedClaims() { return statedClaims; }
}
