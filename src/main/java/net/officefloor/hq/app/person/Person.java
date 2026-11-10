package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A person on a sheet, as the store holds it. {@code id} is null until first saved. */
public class Person {
    private Long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;
    private int statementCount;
    private String statedTotal;
    private LocalDate statementDate;
    private LocalDate retiredFrom;
    private boolean held;
    private final List<Long> patronIds = new ArrayList<>();
    private final TreeMap<LocalDate, String> datedGrades = new TreeMap<>();

    public Person(Long id, String sheetId, String name, String grade) {
        this(id, sheetId, name, grade, 0);
    }

    public Person(Long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    /** The first grade they were given: in force from the beginning of time, until their first dated grade. */
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    /** The grades recorded from a date, by from-date: each in force from it until the next. */
    public Map<LocalDate, String> getDatedGrades() { return datedGrades; }
    /** Record the grade from this date, replacing any recorded from the same date. */
    public void setGradeFrom(String grade, LocalDate from) { datedGrades.put(from, grade); }
    /** The grade in force on the date: the latest dated one on or before it, else the first grade. */
    public String gradeOn(LocalDate date) {
        Map.Entry<LocalDate, String> dated = datedGrades.floorEntry(date);
        return dated == null ? grade : dated.getValue();
    }
    /** Raised by one on each save; an edit names the version it was made from. */
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    /** How many statements have been taken for them. */
    public int getStatementCount() { return statementCount; }
    /** The last statement's due-total (n/a when it had none); null before the first. */
    public String getStatedTotal() { return statedTotal; }
    /** The sheet's today when the last statement was taken; null before the first. */
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatements(int count, String statedTotal, LocalDate statementDate) {
        this.statementCount = count;
        this.statedTotal = statedTotal;
        this.statementDate = statementDate;
    }
    /** The date they are retired from; null when not retired. Set at all, they count as retired. */
    public LocalDate getRetiredFrom() { return retiredFrom; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
    public boolean isRetired() { return retiredFrom != null; }
    /** Whether they are on hold: their OPEN claims count in no total until they are released. */
    public boolean isHeld() { return held; }
    public void setHeld(boolean held) { this.held = held; }
    /** The ids of the people they name as patrons, in the order named. */
    public List<Long> getPatronIds() { return patronIds; }
    /** Whether a claim on this date is refused: they are retired from an earlier date. */
    public boolean retiredBefore(LocalDate date) { return retiredFrom != null && date.isAfter(retiredFrom); }
}
