package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A person on a sheet. {@code grades} are their grades (each one of A, B or C) by the date each applies
 *  from, the first from {@link #START} (the beginning of time); {@code version} moves on with each stored edit;
 *  {@code retiredFrom} is the date they are retired from (null while not retired); {@code held} whether
 *  they are on hold, when their OPEN claims count in no live total. */
public class Person {
    /** The from-date of a person's first grade: the beginning of time. */
    public static final LocalDate START = LocalDate.of(1, 1, 1);

    private final long id;
    private final String sheetId;
    private String name;
    private final NavigableMap<LocalDate, String> grades;
    private long version;
    private LocalDate retiredFrom;
    private boolean held;
    private LocalDate recordFrom;

    public Person(long id, String sheetId, String name, NavigableMap<LocalDate, String> grades, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grades = new TreeMap<>(grades);
        this.version = version;
    }

    public Person(long id, String sheetId, String name, NavigableMap<LocalDate, String> grades, long version,
            LocalDate retiredFrom, boolean held) {
        this(id, sheetId, name, grades, version);
        this.retiredFrom = retiredFrom;
        this.held = held;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    /** The grade in force on the date: the one with the latest from-date on or before it. */
    public String gradeOn(LocalDate on) {
        Map.Entry<LocalDate, String> e = grades.floorEntry(on);
        return e == null ? grades.firstEntry().getValue() : e.getValue();
    }
    /** Every grade by the date it applies from ({@link #START} for the first). */
    public NavigableMap<LocalDate, String> getGrades() { return grades; }
    /** The from-date of the grade the next save records, null when it records none. */
    public LocalDate getRecordFrom() { return recordFrom; }
    public long getVersion() { return version; }
    public LocalDate getRetiredFrom() { return retiredFrom; }
    /** Retired as soon as a date is set, whatever the date. */
    public boolean isRetired() { return retiredFrom != null; }
    public boolean isHeld() { return held; }
    public void setHeld(boolean held) { this.held = held; }
    public void setName(String name) { this.name = name; }
    /** Records the grade from the date at the next save, keeping the others. */
    public void recordGrade(LocalDate from, String grade) {
        grades.put(from, grade);
        recordFrom = from;
    }
    public void setVersion(long version) { this.version = version; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
}
