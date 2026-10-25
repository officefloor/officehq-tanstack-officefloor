package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person on a sheet. Mutable so an Apply step can change it in place. */
public class Person {

    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private List<GradeFrom> grades = List.of();
    private long version;
    private Statements statements = Statements.NONE;
    private LocalDate retiredFrom;
    private List<HoldSpan> holds = List.of();
    private List<String> patrons = List.of();

    public Person(long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public long getId() {
        return id;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** The grade in force on the sheet's today. */
    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    /** The grades the person was given, each from its from-date (the first from the beginning of time). */
    public List<GradeFrom> getGrades() {
        return grades;
    }

    public void setGrades(List<GradeFrom> grades) {
        this.grades = grades;
    }

    /** The grade in force on the date: the one with the latest from-date on or before it, none counting as the earliest. */
    public String gradeOn(LocalDate on) {
        GradeFrom found = null;
        for (GradeFrom g : grades) {
            if (g.from() != null && g.from().isAfter(on)) {
                continue;
            }
            if (found == null || (g.from() != null && (found.from() == null || g.from().isAfter(found.from())))) {
                found = g;
            }
        }
        return found == null ? grade : found.grade();
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /** The person's statements: how many, and the last one's due-total and date. */
    public Statements getStatements() {
        return statements;
    }

    public void setStatements(Statements statements) {
        this.statements = statements;
    }

    /** The date the person is retired from; null when not retired. Set at all, the person counts as retired. */
    public LocalDate getRetiredFrom() {
        return retiredFrom;
    }

    public void setRetiredFrom(LocalDate retiredFrom) {
        this.retiredFrom = retiredFrom;
    }

    public boolean isRetired() {
        return retiredFrom != null;
    }

    /** Whether the person has any hold. */
    public boolean isHeld() {
        return !holds.isEmpty();
    }

    /** The person's holds, in the order they were added. */
    public List<HoldSpan> getHolds() {
        return holds;
    }

    public void setHolds(List<HoldSpan> holds) {
        this.holds = holds;
    }

    /** Whether a hold covers the date: the person's OPEN and PENDING claims dated then, unless a statement binds them,
     *  count in no live total. */
    public boolean heldOn(LocalDate on) {
        return holds.stream().anyMatch(h -> h.covers(on));
    }

    /** The names of the person's patrons, sorted. */
    public List<String> getPatrons() {
        return patrons;
    }

    public void setPatrons(List<String> patrons) {
        this.patrons = patrons;
    }
}
