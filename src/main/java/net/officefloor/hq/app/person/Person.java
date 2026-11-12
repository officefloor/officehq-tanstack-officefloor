package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A person on a sheet. {@code grade} is one of A, B or C; {@code version} moves on with each stored edit;
 *  {@code retiredFrom} is the date they are retired from (null while not retired). */
public class Person {
    private final long id;
    private final String sheetId;
    private String name;
    private String grade;
    private long version;
    private LocalDate retiredFrom;

    public Person(long id, String sheetId, String name, String grade, long version) {
        this.id = id;
        this.sheetId = sheetId;
        this.name = name;
        this.grade = grade;
        this.version = version;
    }

    public Person(long id, String sheetId, String name, String grade, long version, LocalDate retiredFrom) {
        this(id, sheetId, name, grade, version);
        this.retiredFrom = retiredFrom;
    }

    public long getId() { return id; }
    public String getSheetId() { return sheetId; }
    public String getName() { return name; }
    public String getGrade() { return grade; }
    public long getVersion() { return version; }
    public LocalDate getRetiredFrom() { return retiredFrom; }
    /** Retired as soon as a date is set, whatever the date. */
    public boolean isRetired() { return retiredFrom != null; }
    public void setName(String name) { this.name = name; }
    public void setGrade(String grade) { this.grade = grade; }
    public void setVersion(long version) { this.version = version; }
    public void setRetiredFrom(LocalDate retiredFrom) { this.retiredFrom = retiredFrom; }
}
